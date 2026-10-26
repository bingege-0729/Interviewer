package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.RedisKeyManager;
import net.xdclass.config.SmsClient;
import net.xdclass.controller.req.AccountLoginReq;
import net.xdclass.controller.req.SendCheckCodeReq;
import net.xdclass.dto.AccountDTO;
import net.xdclass.dto.AccountLoginResultDTO;
import net.xdclass.dto.SmsDTO;
import net.xdclass.enums.AccountRoleEnum;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.StatusEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.AccountLoginInterceptor;
import net.xdclass.mapper.AccountMapper;
import net.xdclass.model.AccountDO;
import net.xdclass.service.AccountService;
import net.xdclass.service.FileService;
import net.xdclass.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Autowired
    private CaptchaUtil captchaUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SmsClient smsClient;

    @Autowired
    private AccountMapper accountMapper;


    @Autowired
    private FileService fileService;

    /**
     * 验证码过期时间（分钟）
     */
    private final static int CHECK_CODE_EXPIRED_TIME = 10;

    /**
     * 短信发送间隔（毫秒）
     */
    private final static long SEND_INTERVAL_MS = 60 * 1000L;

    @Override
    public JsonData sendCheckCode(SendCheckCodeReq req) {

        log.info("发送验证码请求参数：{}", req);
        String identifier = req.getIdentifier();
        String type = req.getType();

        // 1. 校验图形验证码
        captchaUtil.verifyCaptcha(identifier, req.getCaptcha(), type);

        // 2. 频率限制检查
        String cacheKey = String.format(RedisKeyManager.CHECK_CODE_KEY,  type,identifier);
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cacheValue)) {
            String[] parts = cacheValue.split("_");
            if (parts.length > 1) {
                long lastSendTime = Long.parseLong(parts[1]);
                if (CommonUtil.getCurrentTimestamp() - lastSendTime < SEND_INTERVAL_MS) {
                    log.warn("验证码发送过快, identifier:{}", identifier);
                    return JsonData.buildResult(BizCodeEnum.CODE_LIMITED);
                }
            }
        }

        // 3. 生成6位数字验证码
        String smsCode = CommonUtil.generateNumberCode();

        // 4. 执行发送逻辑
        boolean isSuccess = false;
        if (CommonUtil.isEmail(identifier)) {
            // TODO: 邮件发送逻辑，目前仅打印日志
            log.info("准备发送邮件验证码：{}, identifier:{}", smsCode, identifier);
            isSuccess = true; // 暂定成功
        } else if (CommonUtil.isPhone(identifier)) {
            isSuccess = doSendSms(identifier, smsCode);
        } else {
            return JsonData.buildResult(BizCodeEnum.CODE_TO_ERROR);
        }

        // 5. 发送成功后存入Redis
        if (isSuccess) {
            String value = smsCode + "_" + CommonUtil.getCurrentTimestamp();
            stringRedisTemplate.opsForValue().set(cacheKey, value, CHECK_CODE_EXPIRED_TIME, TimeUnit.MINUTES);
            return JsonData.buildSuccess("验证码发送成功");
        } else {
            log.error("验证码发送失败, identifier:{}", identifier);
            return JsonData.buildCodeAndMsg(BizCodeEnum.SYSTEM_ERROR.getCode(), "验证码发送失败，请检查手机号或稍后再试");
        }
    }

    /**
     * 1、验证短信验证码
     * 2、查询用户唯一标识是否存在数据库
     * 3、生成token
     * @param req
     * @return
     */
    @Override
    public AccountLoginResultDTO login(AccountLoginReq req) {

        //校验短信
        boolean verifyResult = verifyIdentifierCode(req.getIdentifier(), req.getCheckCode(),req.getType());
        if(!verifyResult){
            throw  new BizException(BizCodeEnum.SMS_CODE_ERROR);
        }

        AccountDO accountDO = accountMapper.selectOne(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getStatus, StatusEnum.ON)
                .eq(AccountDO::getEmail, req.getIdentifier()).or().eq(AccountDO::getPhone, req.getIdentifier()));

        if(accountDO == null){
            //用户不存在，注册
            boolean isPhone = CommonUtil.isPhone(req.getIdentifier());

            accountDO = AccountDO.builder().role(AccountRoleEnum.COMMON.name()).username("user_" + req.getIdentifier()).build();
            if(isPhone){
                accountDO.setPhone(req.getIdentifier());
            }else {
                accountDO.setEmail(req.getIdentifier());
            }
            accountMapper.insert(accountDO);
            log.info("用户注册成功：{}", accountDO);
        }

        AccountDTO accountDTO = SpringBeanUtil.copyProperties(accountDO, AccountDTO.class);

        //生成token
        String loginJWT = JwtUtil.geneTenantAccountLoginJWT(accountDTO);

        return AccountLoginResultDTO.builder().success(true).token(loginJWT).accountDTO(accountDTO).build();
    }

    @Override
    public AccountDTO detail() {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        AccountDO accountDO = accountMapper.selectOne(new LambdaQueryWrapper<AccountDO>().eq(AccountDO::getId, accountDTO.getId()));
        if (accountDO == null) {
            log.info("用户不存在：{}", accountDTO);
            throw new BizException(BizCodeEnum.ACCOUNT_UNREGISTER);
        }
        return SpringBeanUtil.copyProperties(accountDO, AccountDTO.class);
    }

    @Override
    public JsonData uploadAvatar(MultipartFile file) {
        //获取当前用户
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        Long accountId = accountDTO.getId();

        //调用文件上传
        JsonData uploadResult = fileService.uploadPublicFile(file);
        if(uploadResult.getCode() != 0){
            log.error("文件上传失败：{}", uploadResult);
            return uploadResult;
        }
        //获取上传后的的文件地址
        Map<String,Object> resultData = (Map<String,Object>)uploadResult.getData();
        String fileUrl = resultData.get("url").toString();
        log.info("文件上传成功，文件地址：{}", fileUrl);

        //更新数据库
        int updated = accountMapper.update(AccountDO.builder().headImg(fileUrl).build(), new LambdaUpdateWrapper<AccountDO>().eq(AccountDO::getId, accountId));
        if(updated <= 0){
         log.error("更新用户头像失败，用户ID：{}", accountId);
         throw new BizException(BizCodeEnum.SYSTEM_ERROR);
        }

        log.info("更新用户头像成功，用户ID：{}", accountId);
        return JsonData.buildSuccess(fileUrl);
    }

    private boolean verifyIdentifierCode(String identifier, String checkCode, String type) {
        String cacheKey = String.format(RedisKeyManager.CHECK_CODE_KEY,type,identifier);
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if(StringUtils.isNotBlank(cacheValue)){
            String[] parts = cacheValue.split("_");
            String cacheCode = parts[0];
            if(cacheCode.equals(checkCode)){
                stringRedisTemplate.delete(cacheKey);
                return true;
            }
        }

        return false;
    }


    /**
     * 封装短信发送逻辑
     * @param phone 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    private boolean doSendSms(String phone, String code) {
        try {
            log.info("发送短信验证码：{}, phone:{}", code, phone);
            System.out.println("发送短信验证码："+code+", phone:"+ phone);
            SmsDTO smsDTO = SmsDTO.builder()
                    .mobile(phone)
                    .param("**code**:" + code + ",**minute**:" + CHECK_CODE_EXPIRED_TIME)
                    .build();

            String body = smsClient.sendSms(smsDTO);
            log.info("[发送短信结果] phone: {}, resp: {}", phone, body);

            if (StringUtils.isBlank(body)) {
                return false;
            }

            Map<String, Object> map = JsonUtil.json2Obj(body, Map.class);
            if (map != null && "0".equals(String.valueOf(map.get("code")))) {
                return true;
            }
            return true;
        } catch (Exception e) {
            log.error("短信发送系统异常, phone:{}", phone, e);
        }
        return false;
    }
}
