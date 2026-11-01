package net.abing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.enums.BenefitTaskStateEnum;
import net.abing.feign.AccountBenefitFeign;
import net.abing.req.BenefitTaskUpdateReq;
import net.abing.service.BenefitTaskService;
import net.abing.util.JsonData;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BenefitTaskServiceImpl implements BenefitTaskService {


    private final AccountBenefitFeign accountBenefitFeign;


    @Override
    public boolean markFinish(String messageId, String businessId, String businessType) {

        return updateTaskStatus(messageId, businessId, businessType, BenefitTaskStateEnum.FINISH.name(), null);

    }

    @Override
    public boolean rollback(String messageId, String businessId, String businessType, String failReason) {
        return updateTaskStatus(messageId, businessId, businessType, BenefitTaskStateEnum.CANCEL.name(), failReason);
    }

    private boolean updateTaskStatus(String messageId, String businessId, String businessType, String operation,String failReason) {

        try {
            BenefitTaskUpdateReq req = BenefitTaskUpdateReq.builder()
                    .messageId(messageId)
                    .businessId(businessId)
                    .businessType(businessType)
                    .operation(operation)
                    .failReason(failReason)
                    .build();

            JsonData jsonData = accountBenefitFeign.updateTaskStatus(req);
            log.info("AiService远程调用AccountService更新任务状态结果: {}", jsonData);
            return jsonData.getCode() == 0;

        }catch (Exception e){
            log.error("AiService远程调用AccountService更新任务状态异常: {}", e.getMessage());
            return false;
        }
    }
}
