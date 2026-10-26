package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BenefitTaskStateEnum;
import net.xdclass.mapper.AccountBenefitMapper;
import net.xdclass.mapper.BenefitTaskMapper;
import net.xdclass.model.AccountBenefitDO;
import net.xdclass.model.BenefitTaskDO;
import net.xdclass.req.BenefitTaskUpdateReq;
import net.xdclass.service.BenefitTaskService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void checkAndCompensate(String messageId, Integer checkLevel) {

        //查询任务记录
        BenefitTaskDO benefitTaskDO = benefitTaskMapper.selectOne(new LambdaQueryWrapper<BenefitTaskDO>().eq(BenefitTaskDO::getMessageId, messageId));
        if (benefitTaskDO == null) {
            log.error("消息id对应的记录不存在 {}", messageId);
            return;
        }
        //获取枚举状态
        BenefitTaskStateEnum currentStatus = BenefitTaskStateEnum.valueOf(benefitTaskDO.getLockState());
        log.info("触发第{}次检查 ，任务内容:{}", checkLevel, benefitTaskDO);

        //判断状态
        if (currentStatus.isFinish()) {
            log.info("任务已经完成，无需补偿 {}", benefitTaskDO);
            return;
        }
        if(currentStatus.isCancel()){
            log.info("任务已经取消，无需补偿 {}", benefitTaskDO);
            return;
        }

        // lock状态下，判断是不是第二次检查
        if (currentStatus.isLock() && checkLevel >= 2) {
            log.info("任务已经锁定，且是第二次检查， {}", benefitTaskDO);

            //恢复权益
            compensateBenefit(benefitTaskDO);

        }else {
            log.info("第一次检查，状态是lock，任务正在处理中，请稍等 {}", benefitTaskDO);
        }
    }

    private void compensateBenefit(BenefitTaskDO benefitTaskDO) {
        log.info("开始恢复权益 {}", benefitTaskDO);
        AccountBenefitDO accountBenefitDO = accountBenefitMapper.selectById(benefitTaskDO.getAccountBenefitId());
        accountBenefitDO.setRemainingCount(accountBenefitDO.getRemainingCount() + benefitTaskDO.getUseTimes());
        accountBenefitMapper.updateById(accountBenefitDO);
        log.info("恢复权益成功 {}", accountBenefitDO);

    }

    @Override
    public boolean updateTaskStatus(BenefitTaskUpdateReq req) {
        //查询任务记录
        BenefitTaskDO benefitTaskDO = benefitTaskMapper.selectOne(new LambdaQueryWrapper<BenefitTaskDO>().eq(BenefitTaskDO::getMessageId, req.getMessageId()));
        if (benefitTaskDO == null) {
            log.info("消息id对应的记录不存在 {}", req.getMessageId());
            return false;
        }

        //获取枚举状态
        BenefitTaskStateEnum currentStatus = BenefitTaskStateEnum.valueOf(benefitTaskDO.getLockState());
        log.info("任务当前状态 {}", currentStatus);

        //判断状态
        if (currentStatus.isFinish()) {
            log.info("任务已经完成，无需补偿 {}", benefitTaskDO);
            return true; //认为是成功的
        }
        if(currentStatus.isCancel()){
            log.info("任务已经取消，无需补偿 {}", benefitTaskDO);
            return true;
        }

        //更加操作类型更新状态
        if(BenefitTaskStateEnum.FINISH.getCode().equals(req.getOperation())){
            //标记完成
            benefitTaskDO.setLockState(BenefitTaskStateEnum.FINISH.name());
            benefitTaskMapper.updateById(benefitTaskDO);
            log.info("任务标记完成 {}", benefitTaskDO);
            return true;
        } else if(BenefitTaskStateEnum.CANCEL.getCode().equals(req.getOperation())){
            //回滚补偿权益
            compensateBenefit(benefitTaskDO);

            //标记取消
            benefitTaskDO.setLockState(BenefitTaskStateEnum.CANCEL.name());
            benefitTaskMapper.updateById(benefitTaskDO);
            log.info("任务标记取消 {}", benefitTaskDO);
            return true;
        }else {
            log.info("无效的操作 {},任务:{}", req.getOperation(), benefitTaskDO);
            return false;
        }
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

        benefitTaskMapper.insert(taskDO);

        log.info("保存task记录成功 {}", taskDO);

        return taskDO;
    }
}
