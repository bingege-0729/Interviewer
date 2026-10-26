package net.xdclass.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.BenefitDelayCheckMQConfig;
import net.xdclass.controller.req.AccountBenefitPageReq;
import net.xdclass.dto.AccountBenefitDTO;
import net.xdclass.dto.BenefitCheckResultDTO;
import net.xdclass.dto.BenefitDelayCheckMessageDTO;
import net.xdclass.dto.BenefitGrantMessage;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.interceptor.AccountLoginInterceptor;
import net.xdclass.dto.AccountDTO;
import net.xdclass.mapper.AccountBenefitMapper;
import net.xdclass.model.AccountBenefitDO;
import net.xdclass.model.BenefitTaskDO;
import net.xdclass.req.BenefitCheckReq;
import net.xdclass.service.AccountBenefitService;
import net.xdclass.service.BenefitTaskService;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountBenefitServiceImpl implements AccountBenefitService {

    private final AccountBenefitMapper accountBenefitMapper;


    private final BenefitTaskService benefitTaskService;


    private final RabbitTemplate rabbitTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantBenefit(BenefitGrantMessage message) {

        log.info("开始发放权益：{}",message);
        List<BenefitGrantMessage.BenefitItem> benefitItems = message.getBenefitItems();
        if(benefitItems != null && !benefitItems.isEmpty()){
            for (BenefitGrantMessage.BenefitItem benefitItem : benefitItems) {
                log.info("发放权益：{}",benefitItem);
                grantSingleItem(message, benefitItem);
            }
        }


    }

    /**
     * 检查并扣减权益
     *
     * 设计说明：
     * - 权益扣减和任务保存在同一事务中，保证数据一致性
     * - 返回messageId供ai-service发送业务MQ使用
     *
     * 事务回滚 → MQ消息已发送 → 消费时查不到任务记录 → 跳过处理 ✓
     * 事务提交 → MQ消息已发送 → 消费时查到任务记录 → 正常处理 ✓
     * @param benefitCheckReq 权益检查请求
     * @return 扣减结果（包含messageId）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JsonData checkAndDeductBenefit(BenefitCheckReq benefitCheckReq) {
        //查询权益，优先使用即将过期
        List<AccountBenefitDO> accountBenefitDOS = accountBenefitMapper.selectList(new LambdaQueryWrapper<AccountBenefitDO>()
                .eq(AccountBenefitDO::getAccountId, benefitCheckReq.getAccountId())
                .eq(AccountBenefitDO::getBenefitCode, benefitCheckReq.getBenefitCode())
                .gt(AccountBenefitDO::getRemainingCount, 0)
                .gt(AccountBenefitDO::getEndTime, new Date())
                .orderByAsc(AccountBenefitDO::getEndTime)
        );

        if(accountBenefitDOS.isEmpty()){
            log.warn("用户没有该权益或者权益已用完", benefitCheckReq);
            return JsonData.buildResult(BizCodeEnum.BENEFIT_NOT_ENOUGH);
        }

        //创建权益任务记录表
        AccountBenefitDO accountBenefitDO = accountBenefitDOS.getFirst();
        int count = benefitCheckReq.getCount()!=null ? benefitCheckReq.getCount():1;

        BenefitTaskDO task = benefitTaskService.saveTask(
                benefitCheckReq.getBusinessId(),
                benefitCheckReq.getBenefitCode(),
                benefitCheckReq.getAccountId(),
                accountBenefitDO.getId(),
                count
                );

        //发送延迟消息
        sendDelayCheckMessage(task.getMessageId(),benefitCheckReq.getBusinessId());

        //扣减权益
        accountBenefitDO.setRemainingCount(accountBenefitDO.getRemainingCount() - count);
        accountBenefitMapper.updateById(accountBenefitDO);
        log.info("扣减权益成功：{}", accountBenefitDO);

        BenefitCheckResultDTO benefitCheckResultDTO = BenefitCheckResultDTO.builder()
                .success(true)
                .messageId(task.getMessageId())
                .businessId(benefitCheckReq.getBusinessId())
                .benefitCode(benefitCheckReq.getBenefitCode())
                .accountBenefitId(accountBenefitDO.getId())
                .remainingCount(accountBenefitDO.getRemainingCount())
                .deductedCount(count)
                .build();
        //构建结果返回
        return JsonData.buildSuccess(benefitCheckResultDTO);
    }

    private void sendDelayCheckMessage(String messageId, String businessId) {


        //第1次检查
        BenefitDelayCheckMessageDTO firstDelayCheckMessageDTO = BenefitDelayCheckMessageDTO.builder()
                .messageId(messageId)
                .businessId(businessId)
                .checkLevel(1)
                .build();
        rabbitTemplate.convertAndSend(BenefitDelayCheckMQConfig.BENEFIT_DELAY_CHECK_EXCHANGE,
                BenefitDelayCheckMQConfig.DELAY_CHECK_ROUTING_KEY_1MIN,
                firstDelayCheckMessageDTO
              );
        log.info("发送第1次权益检查延迟消息，messageId:{}", messageId);

        //第二次
        BenefitDelayCheckMessageDTO secondDelayCheckMessageDTO = BenefitDelayCheckMessageDTO.builder()
                .messageId(messageId)
                .businessId(businessId)
                .checkLevel(2)
                .build();
        rabbitTemplate.convertAndSend(BenefitDelayCheckMQConfig.BENEFIT_DELAY_CHECK_EXCHANGE,
                BenefitDelayCheckMQConfig.DELAY_CHECK_ROUTING_KEY_5MIN,
                secondDelayCheckMessageDTO
              );
        log.info("发送第2次权益检查延迟消息，messageId:{}", messageId);

    }

    private void grantSingleItem(BenefitGrantMessage message, BenefitGrantMessage.BenefitItem benefitItem) {

        String benefitCode = benefitItem.getBenefitCode();
        Integer count = benefitItem.getCount();
        //需要做幂等检查

        Long selectCount = accountBenefitMapper.selectCount(new LambdaQueryWrapper<AccountBenefitDO>()
                .eq(AccountBenefitDO::getProductOrderId, message.getOrderId())
                .eq(AccountBenefitDO::getBenefitId, benefitItem.getBenefitId())
        );
        if(selectCount > 0){
            log.info("幂等检查通过，订单ID:{}, 权益ID:{}",message.getOrderId(),benefitItem.getBenefitId());
            return;
        }


        //每次购买插入新纪录，保持独立的有效期

        AccountBenefitDO accountBenefitDO = AccountBenefitDO.builder()
                .accountId(message.getAccountId())
                .benefitId(benefitItem.getBenefitId())
                .benefitCode(benefitCode)
                .productOrderId(message.getOrderId())
                .orderType(message.getOrderType())
                .remainingCount(count)
                .totalCount(count)
                .startTime(new Date())
                //当前时间，加上有效天数
                .endTime(calculateExpireDays(new Date(), benefitItem.getValidDays()))
                .build();

        accountBenefitMapper.insert(accountBenefitDO);
    }

    /**
     * 计算权益到期天数
     */
    private Date calculateExpireDays(Date date,int validDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, validDays);
        return calendar.getTime();
    }

    /**
     * 权益编码到名称的映射
     */
    private static final Map<String, String> BENEFIT_NAME_MAP = Map.of(
            "COMMON_INTERVIEW", "简历综合面",
            "SPECIAL_INTERVIEW", "专项面试",
            "RESUME_ANALYSE", "AI简历分析",
            "QUESTION_BANK", "面试题库"
    );

    /**
     * 权益编码到描述的映射
     */
    private static final Map<String, String> BENEFIT_DESC_MAP = Map.of(
            "COMMON_INTERVIEW", "基于简历直接进行面试，最高频题目",
            "SPECIAL_INTERVIEW", "针对特定技术领域生成面试题并模拟面试",
            "RESUME_ANALYSE", "AI系统分析简历优缺点，提供针对性优化建议",
            "QUESTION_BANK", "互联网公司面试题库大集合"
    );


    @Override
    public JsonData page(AccountBenefitPageReq req) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        LambdaQueryWrapper<AccountBenefitDO> queryWrapper = new LambdaQueryWrapper<AccountBenefitDO>()
                .eq(AccountBenefitDO::getAccountId, accountDTO.getId())
                .orderByDesc(AccountBenefitDO::getGmtCreate);

        Page<AccountBenefitDO> page = accountBenefitMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()), queryWrapper);

        // DO转DTO，补充benefitName和benefitDescription
        List<AccountBenefitDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = CommonUtil.convertToPageMap(page, dtoList);
        return JsonData.buildSuccess(result);
    }

    @Override
    public JsonData listByAccountId(Long accountId) {
        log.info("查询用户有效权益列表, accountId:{}", accountId);

        // 查询未过期且剩余次数>0的权益
        List<AccountBenefitDO> benefitList = accountBenefitMapper.selectList(
                new LambdaQueryWrapper<AccountBenefitDO>()
                        .eq(AccountBenefitDO::getAccountId, accountId)
                        .gt(AccountBenefitDO::getRemainingCount, 0)
                        .gt(AccountBenefitDO::getEndTime, new Date())
                        .orderByAsc(AccountBenefitDO::getEndTime)
        );

        // 按权益编码分组汇总，同类权益合并剩余次数
        Map<String, Integer> benefitSummary = benefitList.stream()
                .collect(Collectors.groupingBy(
                        AccountBenefitDO::getBenefitCode,
                        Collectors.summingInt(AccountBenefitDO::getRemainingCount)
                ));

        // 构建简洁的权益摘要列表
        List<Map<String, Object>> summaryList = benefitSummary.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("benefitCode", entry.getKey());
                    item.put("benefitName", BENEFIT_NAME_MAP.getOrDefault(entry.getKey(), entry.getKey()));
                    item.put("remainingCount", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return JsonData.buildSuccess(summaryList);
    }

    private AccountBenefitDTO convertToDTO(AccountBenefitDO benefitDO) {
        return AccountBenefitDTO.builder()
                .id(benefitDO.getId())
                .accountId(benefitDO.getAccountId())
                .benefitCode(benefitDO.getBenefitCode())
                .benefitName(BENEFIT_NAME_MAP.getOrDefault(benefitDO.getBenefitCode(), "权益"))
                .benefitDescription(BENEFIT_DESC_MAP.getOrDefault(benefitDO.getBenefitCode(), ""))
                .productOrderId(benefitDO.getProductOrderId())
                .remainingCount(benefitDO.getRemainingCount())
                .totalCount(benefitDO.getTotalCount())
                .startTime(benefitDO.getStartTime())
                .endTime(benefitDO.getEndTime())
                .gmtCreate(benefitDO.getGmtCreate())
                .gmtModified(benefitDO.getGmtModified())
                .build();
    }


}
