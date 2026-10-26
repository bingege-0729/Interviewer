package net.xdclass.service;

import net.xdclass.model.BenefitTaskDO;
import net.xdclass.req.BenefitTaskUpdateReq;

public interface BenefitTaskService {
    void checkAndCompensate(String messageId, Integer checkLevel);

    boolean updateTaskStatus(BenefitTaskUpdateReq req);

    BenefitTaskDO saveTask(String businessId, String benefitCode, Long accountId, Long id, int count);
}
