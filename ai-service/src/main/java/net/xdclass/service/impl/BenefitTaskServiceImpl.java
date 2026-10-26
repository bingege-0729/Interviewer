package net.xdclass.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BenefitTaskStateEnum;
import net.xdclass.feign.AccountBenefitFeign;
import net.xdclass.req.BenefitTaskUpdateReq;
import net.xdclass.service.BenefitTaskService;
import net.xdclass.util.JsonData;
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
