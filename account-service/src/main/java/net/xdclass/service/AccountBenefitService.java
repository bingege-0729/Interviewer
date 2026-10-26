package net.xdclass.service;

import net.xdclass.controller.req.AccountBenefitPageReq;
import net.xdclass.dto.BenefitGrantMessage;
import net.xdclass.req.BenefitCheckReq;
import net.xdclass.util.JsonData;

public interface AccountBenefitService {
    void grantBenefit(BenefitGrantMessage message);

    JsonData checkAndDeductBenefit(BenefitCheckReq benefitCheckReq);

    /**
     * 分页查询当前用户的权益列表
     */
    JsonData page(AccountBenefitPageReq req);

    JsonData listByAccountId(Long accountId);
}
