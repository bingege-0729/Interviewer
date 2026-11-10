package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import net.abing.enums.BenefitTaskStateEnum;
import net.abing.mapper.AccountBenefitMapper;
import net.abing.mapper.BenefitTaskMapper;
import net.abing.model.AccountBenefitDO;
import net.abing.model.BenefitTaskDO;
import net.abing.req.BenefitTaskUpdateReq;
import net.abing.service.BenefitTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class BenefitTaskServiceImpl implements BenefitTaskService {

    @Autowired
    private BenefitTaskMapper benefitTaskMapper;

    @Autowired
    private AccountBenefitMapper accountBenefitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndCompensate(String messageId, Integer checkLevel) {

        BenefitTaskDO benefitTaskDO = selectByMessageId(messageId);
        if (benefitTaskDO == null) {
            log.error("消息id对应的记录不存在 {}", messageId);
            return;
        }

        log.info("触发第{}次检查，任务内容:{}", checkLevel, benefitTaskDO);

        // 第一次检查只观察，给业务留出创建面试的时间，不急着退回权益
        if (checkLevel == null || checkLevel < 2) {
            log.info("第1次检查，任务仍为锁定状态，继续等待业务结果, messageId:{}", messageId);
            return;
        }

        // 把「推进状态」本身当作锁：只有真正把 LOCK 改成 CANCEL 的那一次才有资格退权益。
        // 1min 和 5min 两条延迟消息是独立投递的，basicNack 也允许重投，
        // 先查状态再改的写法会让同一个 messageId 进来两次，把次数加回两遍。
        if (!transitState(messageId, BenefitTaskStateEnum.LOCK, BenefitTaskStateEnum.CANCEL)) {
            log.info("任务已被其他检查推进，跳过本次补偿, messageId:{}, 当前状态:{}",
                    messageId, benefitTaskDO.getLockState());
            return;
        }

        compensateBenefit(benefitTaskDO);
        log.info("第二次检查任务仍未完成，已回滚权益, messageId:{}", messageId);
    }

    /**
     * 推进任务状态，并把「原状态」作为更新条件。
     * 返回 true 表示本次调用抢到了状态流转权，false 表示已经被别人推进过。
     */
    private boolean transitState(String messageId, BenefitTaskStateEnum from, BenefitTaskStateEnum to) {
        int affected = benefitTaskMapper.update(null,
                new LambdaUpdateWrapper<BenefitTaskDO>()
                        .set(BenefitTaskDO::getLockState, to.name())
                        .eq(BenefitTaskDO::getMessageId, messageId)
                        .eq(BenefitTaskDO::getLockState, from.name()));
        return affected > 0;
    }

    private BenefitTaskDO selectByMessageId(String messageId) {
        return benefitTaskMapper.selectOne(
                new LambdaQueryWrapper<BenefitTaskDO>().eq(BenefitTaskDO::getMessageId, messageId));
    }

    /**
     * 退回权益次数。用相对值更新，并用 LEAST 兜住总数上限，
     * 即使补偿链路将来再出问题，也不会把可用次数加超。
     */
    private void compensateBenefit(BenefitTaskDO benefitTaskDO) {
        int affected = accountBenefitMapper.update(null,
                new LambdaUpdateWrapper<AccountBenefitDO>()
                        .setSql("remaining_count = LEAST(remaining_count + "
                                + benefitTaskDO.getUseTimes() + ", total_count)")
                        .eq(AccountBenefitDO::getId, benefitTaskDO.getAccountBenefitId()));
        if (affected > 0) {
            log.info("恢复权益成功, accountBenefitId:{}, 次数:+{}",
                    benefitTaskDO.getAccountBenefitId(), benefitTaskDO.getUseTimes());
        } else {
            log.error("恢复权益失败，权益记录不存在, accountBenefitId:{}", benefitTaskDO.getAccountBenefitId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskStatus(BenefitTaskUpdateReq req) {

        BenefitTaskDO benefitTaskDO = selectByMessageId(req.getMessageId());
        if (benefitTaskDO == null) {
            log.info("消息id对应的记录不存在 {}", req.getMessageId());
            return false;
        }

        log.info("任务当前状态 {}, 本次操作:{}", benefitTaskDO.getLockState(), req.getOperation());

        if (BenefitTaskStateEnum.FINISH.getCode().equals(req.getOperation())) {
            // LOCK → FINISH。抢不到说明已是终态，重复回调按成功处理，保证接口幂等
            if (!transitState(req.getMessageId(), BenefitTaskStateEnum.LOCK, BenefitTaskStateEnum.FINISH)) {
                log.info("任务已是终态，忽略重复的完成回调, messageId:{}", req.getMessageId());
                return true;
            }
            log.info("任务标记完成 {}", req.getMessageId());
            return true;
        }

        if (BenefitTaskStateEnum.CANCEL.getCode().equals(req.getOperation())) {
            // 先抢状态再退权益：抢到的人才负责退，抢不到的直接返回，避免重复加次数
            if (!transitState(req.getMessageId(), BenefitTaskStateEnum.LOCK, BenefitTaskStateEnum.CANCEL)) {
                log.info("任务已是终态，忽略重复的取消回调, messageId:{}", req.getMessageId());
                return true;
            }
            compensateBenefit(benefitTaskDO);
            log.info("任务标记取消并回滚权益 {}", req.getMessageId());
            return true;
        }

        log.info("无效的操作 {},任务:{}", req.getOperation(), req.getMessageId());
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitTaskDO saveTask(String businessId, String benefitCode, Long accountId, Long accountBenefitId, int count) {

        //生成消息的唯一id
        String messageId = UUID.randomUUID().toString().replace("-", "");
        BenefitTaskDO taskDO = BenefitTaskDO.builder().messageId(messageId)
                .businessId(businessId)
                .benefitCode(benefitCode)
                .accountId(accountId)
                .accountBenefitId(accountBenefitId)
                .useTimes(count)
                .lockState(BenefitTaskStateEnum.LOCK.name())
                .build();

        try {
            benefitTaskMapper.insert(taskDO);
        } catch (DuplicateKeyException e) {
            // uk_biz_benefit 拦住的是「同一业务同一权益已经扣过」，属于重复请求而非系统故障。
            // 这里必须原样抛出：包成 RuntimeException 会让全局处理器按 Exception 兜底，
            // 最终返回「系统异常」，而不是我们想要的「重复提交」。
            log.warn("权益已扣减，拒绝重复请求, businessId:{}, benefitCode:{}", businessId, benefitCode);
            throw e;
        }

        log.info("保存task记录成功 {}", taskDO);

        return taskDO;
    }
}
