package net.abing.controller.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageOrderCreateReq {


    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    @NotNull(message = "支付类型不能为空")
    private String payType;

    private BigDecimal discount = BigDecimal.ONE;
}
