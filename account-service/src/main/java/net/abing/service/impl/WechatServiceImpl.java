package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.RedisKeyManager;
import net.abing.config.WechatLoginConfig;
import net.abing.dto.AccountDTO;
import net.abing.dto.AccountLoginResultDTO;
import net.abing.dto.WechatAccountDTO;
import net.abing.dto.WechatQrCodeDTO;
import net.abing.enums.AccountRoleEnum;
import net.abing.enums.BizCodeEnum;
import net.abing.enums.StatusEnum;
import net.abing.enums.WechatSceneQrCodeStatus;
import net.abing.exception.BizException;
import net.abing.mapper.AccountMapper;
import net.abing.model.AccountDO;
import net.abing.service.WechatService;
import net.abing.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    @Autowired
    private WechatLoginConfig wechatLoginConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Autowired
    private AccountMapper accountMapper;


    @Override
    public String getAccessToken() {

        try {

            //1 先从redis获取
            String cacheToken = (String) redisTemplate.opsForValue().get(String.format(RedisKeyManager.WECHAT_ACCESS_TOKEN, wechatLoginConfig.getAppId()));
            if (StringUtils.isNotBlank(cacheToken)) {
                log.info("从缓存中获取微信access_token:{}", cacheToken);
                return cacheToken;
            }

            // 2 使用分布式锁，防止高并发情况下多个请求同时获取token
            String lockKey = String.format(RedisKeyManager.WECHAT_ACCESS_TOKEN_LOCK, wechatLoginConfig.getAppId());
            Boolean lockResult = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(lockResult)) {
                try {
                    // 3 double check：再次检查缓存，避免重复请求
                    cacheToken = (String) redisTemplate.opsForValue().get(String.format(RedisKeyManager.WECHAT_ACCESS_TOKEN, wechatLoginConfig.getAppId()));
                    if (StringUtils.isNotBlank(cacheToken)) {
                        log.info("Double check从缓存中获取微信access_token:{}", cacheToken);
                        return cacheToken;
                    }

                    // 4 从微信服务器获取
                    return fetchAccessTokenFromWechat();
                } finally {
                    // 5 释放锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 6 未获取到锁，等待一段时间后重试获取缓存
                log.info("未获取到锁，等待重试获取缓存");
                Thread.sleep(100);
                cacheToken = (String) redisTemplate.opsForValue().get(String.format(RedisKeyManager.WECHAT_ACCESS_TOKEN, wechatLoginConfig.getAppId()));
                if (StringUtils.isNotBlank(cacheToken)) {
                    return cacheToken;
                }
                // 仍然没有获取到，抛出异常
                throw new BizException(BizCodeEnum.SYSTEM_ERROR);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取微信access_token被中断:{}", e.getMessage());
            throw new BizException(BizCodeEnum.SYSTEM_ERROR);
        } catch (Exception e) {
            log.error("获取微信access_token异常", e);
            throw new BizException(BizCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public JsonData generateQrcode() {

        try {
            // 1 生成场景ID
            String sceneId = String.valueOf(System.currentTimeMillis());

            // 2 获取accessToken
            String accessToken = getAccessToken();
            if (StringUtils.isBlank(accessToken)) {
                log.error("获取微信access_token失败");
                return JsonData.buildError("获取微信access_token失败");
            }

            // 3 创建ticket
            String qrCodeUrl = createQrcode(accessToken, sceneId);
            if (StringUtils.isBlank(qrCodeUrl)) {
                log.error("创建微信二维码失败");
                return JsonData.buildResult(BizCodeEnum.SCENE_QRCODE_CREATE_ERROR);
            }


            //4 构建二维码
            WechatQrCodeDTO wechatQrCodeDTO = WechatQrCodeDTO.builder().sceneId(sceneId)
                    .qrCodeUrl(qrCodeUrl)
                    .expiresTime(wechatLoginConfig.getQrCodeExpireTime())
                    .status(WechatSceneQrCodeStatus.WAITING.name())
                    .createTime(System.currentTimeMillis())
                    .build();

            // 5 保存二维码信息
            redisTemplate.opsForValue().
                    set(String.format(RedisKeyManager.WECHAT_SCENE_QRCODE, sceneId), wechatQrCodeDTO,
                            wechatLoginConfig.getQrCodeExpireTime(), TimeUnit.SECONDS);

            log.info("生成二维码成功：{}", wechatQrCodeDTO);
            return JsonData.buildSuccess(wechatQrCodeDTO);
        } catch (Exception e) {
            log.error("生成微信二维码异常", e);
            return JsonData.buildResult(BizCodeEnum.SCENE_QRCODE_CREATE_ERROR);
        }

    }

    @Override
    public JsonData getLoginResult(String sceneId) {
        // 获取二维码状态
        String redisKey = String.format(RedisKeyManager.WECHAT_SCENE_QRCODE, sceneId);
        WechatQrCodeDTO qrCodeDTO = (WechatQrCodeDTO)redisTemplate.opsForValue().get(redisKey);
        if(qrCodeDTO == null){
            return JsonData.buildResult(BizCodeEnum.SCENE_QRCODE_NOT_EXIST);
        }


        //如果不是Confirm状态，就返回当前
        if(!WechatSceneQrCodeStatus.CONFIRMED.name().equals(qrCodeDTO.getStatus())){
            return JsonData.buildSuccess(qrCodeDTO);
        }

        //如果是Confirm状态，就返回用户信息
        String loginResultKey = String.format(RedisKeyManager.WECHAT_LOGIN_RESULT, sceneId);
        AccountLoginResultDTO loginResultDTO = (AccountLoginResultDTO)redisTemplate.opsForValue().get(loginResultKey);

        return JsonData.buildSuccess(loginResultDTO);
    }

    @Override
    public JsonData handleEventCallback(String sceneId, String openId) {

        //* 验证二维码有效性：检查Redis中是否存在对应sceneId的二维码，若不存在则返回错误。
        String redisKey = String.format(RedisKeyManager.WECHAT_SCENE_QRCODE, sceneId);
        WechatQrCodeDTO qrCodeDTO = (WechatQrCodeDTO)redisTemplate.opsForValue().get(redisKey);
        if(qrCodeDTO == null){
            log.error("二维码不存在");
            return JsonData.buildResult(BizCodeEnum.SCENE_QRCODE_NOT_EXIST);
        }
        //* 更新二维码状态：将二维码状态更新为“已扫码”。
        updateQrCodeStatus(sceneId, WechatSceneQrCodeStatus.SCANNED);
        log.info("二维码已扫码");

        //* 获取用户信息：调用微信API获取扫码用户的微信信息。
        WechatAccountDTO  wechatAccountDTO = getWechatAccountInfo(openId);
        log.info("获取用户信息成功：{}", wechatAccountDTO);
        //* 用户登录/注册：根据用户信息执行登录或注册操作，并生成JWT Token。

        AccountLoginResultDTO loginResultDTO = null;
        if (wechatAccountDTO != null) {
            loginResultDTO = loginOrRegister(wechatAccountDTO);
        }
        if(loginResultDTO.isSuccess()){
            //* 更新状态并存储结果：若登录成功，更新二维码状态为“已确认”
            updateQrCodeStatus(sceneId, WechatSceneQrCodeStatus.CONFIRMED);
            log.info("二维码已确认");
            //并将登录结果存入Redis供前端轮询。
            String loginResultKey = String.format(RedisKeyManager.WECHAT_LOGIN_RESULT, sceneId);
            redisTemplate.opsForValue().set(loginResultKey, loginResultDTO,
                    wechatLoginConfig.getLoginStateExpireTime(), TimeUnit.SECONDS);
            log.info("存储登录结果成功：{}", loginResultDTO);
            return JsonData.buildSuccess(loginResultDTO);
        }else {
            log.error("登录失败,openid={},errorsmg={}", openId, loginResultDTO.getErrorMessage());
            return JsonData.buildError(loginResultDTO.getErrorMessage());
        }

    }

    private AccountLoginResultDTO loginOrRegister(WechatAccountDTO wechatAccountDTO) {
        //查询账号
        AccountDO accountDO = accountMapper.selectOne(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getOpenid, wechatAccountDTO.getOpenid()));
        if(accountDO == null){
            accountDO = AccountDO.builder()
                    .username("wx_"+ CommonUtil.generateUUID())
                    .openid(wechatAccountDTO.getOpenid())
                    .unionid(wechatAccountDTO.getUnionid())
                    .status(StatusEnum.ON.name())
                    .role(AccountRoleEnum.COMMON.name())
                    .build();
            accountMapper.insert(accountDO);
            log.info("新用户注册成功：{}", accountDO);
        }
        //生成jwt
        AccountDTO accountDTO = SpringBeanUtil.copyProperties(accountDO, AccountDTO.class);
        String token = JwtUtil.geneTenantAccountLoginJWT(accountDTO);
        log.info("生成jwt成功：{}", token);

        return AccountLoginResultDTO.builder().success(true).token(token).accountDTO(accountDTO).build();
    }

    private WechatAccountDTO getWechatAccountInfo(String openId) {

        try {
            String accessToken = getAccessToken();

            String url = String.format("%s?access_token=%s&openid=%s", wechatLoginConfig.getUserInfoUrl(), accessToken, openId);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            if(jsonNode.has("errcode")){
                log.error("获取微信用户信息失败：{}", jsonNode.get("errmsg"));
                return null;
            }
            WechatAccountDTO wechatAccountDTO = WechatAccountDTO.builder()
                    .openid(jsonNode.get("openid").asText())
                    .unionid(jsonNode.has("unionid")?jsonNode.get("unionid").asText():"")
                    .subscribe(jsonNode.get("subscribe").asInt())
                    .subscribeTime(jsonNode.get("subscribe_time").asLong())
                    .remark(jsonNode.get("remark").asText())
                    .groupid(jsonNode.get("groupid").asInt()).build();

            return wechatAccountDTO;
        }catch (Exception e){
            log.error("获取微信用户信息异常", e);
            return null;
        }
    }

    private void updateQrCodeStatus(String sceneId, WechatSceneQrCodeStatus status) {

        String redisKey = String.format(RedisKeyManager.WECHAT_SCENE_QRCODE, sceneId);
        WechatQrCodeDTO qrCodeDTO = (WechatQrCodeDTO)redisTemplate.opsForValue().get(redisKey);
        if(qrCodeDTO!=null){
            qrCodeDTO.setStatus(status.name());
            redisTemplate.opsForValue().set(redisKey, qrCodeDTO,wechatLoginConfig.getQrCodeExpireTime(), TimeUnit.SECONDS);
        }

    }

    private String createQrcode(String accessToken, String sceneId) {

        //构建请求URL
        try {
            String url = String.format("%s?access_token=%s", wechatLoginConfig.getQrCodeUrl(), accessToken);

            //构建请求体
            Map<String, Object> body = Map.of(
                    "action_name", "QR_STR_SCENE",
                    "expire_seconds", wechatLoginConfig.getQrCodeExpireTime(),
                    "action_info", Map.of("scene", Map.of("scene_str", sceneId))
            );

            String jsonBody = JsonUtil.obj2Json(body);
            log.info("请求微信服务器, url:{}, body:{}", url, jsonBody);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, jsonBody, String.class);
            String responseBody = responseEntity.getBody();
            log.info("微信服务器返回:{}", responseBody);

            //解析结果
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("ticket")) {
                String ticket = jsonNode.get("ticket").asText();
                log.info("创建二维码成功：{}", ticket);
                String qrCodeUrl = String.format("%s?ticket=%s", wechatLoginConfig.getShowQrCodeUrl(), ticket);
                log.info("二维码地址：{}", qrCodeUrl);
                return qrCodeUrl;
            } else {
                log.error("创建二维码失败：{}", jsonNode.get("errmsg"));
                return null;
            }

        } catch (Exception e) {
            log.error("创建二维码异常", e);
        }
        return null;
    }

    /**
     * 从微信服务器获取access_token
     */
    private String fetchAccessTokenFromWechat() {
        try {
            String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s",
                    wechatLoginConfig.getAccessTokenUrl(),
                    wechatLoginConfig.getAppId(),
                    wechatLoginConfig.getAppSecret());

            log.info("从微信服务器获取token, url:{}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.info("微信服务器返回:{}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("errcode")) {
                log.error("微信服务器异常:{}", jsonNode.get("errmsg"));
                throw new BizException(BizCodeEnum.SYSTEM_ERROR);
            }

            String accessToken = jsonNode.get("access_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            log.info("微信access_token:{}, 有效时间:{}", accessToken, expiresIn);

            // 存储到redis，提前5分钟过期
            redisTemplate.opsForValue().set(
                    String.format(RedisKeyManager.WECHAT_ACCESS_TOKEN, wechatLoginConfig.getAppId()),
                    accessToken,
                    expiresIn - 300,
                    TimeUnit.SECONDS);

            return accessToken;
        } catch (Exception e) {
            log.error("从微信服务器获取access_token失败", e);
            throw new BizException(BizCodeEnum.SYSTEM_ERROR);
        }
    }


}
