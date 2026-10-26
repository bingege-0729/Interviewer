package net.xdclass.controller.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BenefitOrderCreateReq {


    @NotNull(message = "权益ID不能为空")
    private Long benefitId;

    @NotNull(message = "购买次数不能为空")
    @Min(value = 1, message = "购买次数不能小于1")
    private Integer purchaseCount;

    @NotNull(message = "支付类型不能为空")
    private String payType;

    private BigDecimal discount = BigDecimal.ONE;

}
