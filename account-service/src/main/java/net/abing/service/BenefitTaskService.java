package net.abing.service;

import net.abing.model.BenefitTaskDO;
import net.abing.req.BenefitTaskUpdateReq;

public interface BenefitTaskService {
    void checkAndCompensate(String messageId, Integer checkLevel);

    boolean updateTaskStatus(BenefitTaskUpdateReq req);

    BenefitTaskDO saveTask(String businessId, String benefitCode, Long accountId, Long id, int count);
}
