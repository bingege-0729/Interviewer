package net.abing.service;

import net.abing.controller.req.AccountBenefitPageReq;
import net.abing.dto.BenefitGrantMessage;
import net.abing.req.BenefitCheckReq;
import net.abing.util.JsonData;

public interface AccountBenefitService {
    void grantBenefit(BenefitGrantMessage message);

    JsonData checkAndDeductBenefit(BenefitCheckReq benefitCheckReq);

    /**
     * 分页查询当前用户的权益列表
     */
    JsonData page(AccountBenefitPageReq req);

    JsonData listByAccountId(Long accountId);
}
