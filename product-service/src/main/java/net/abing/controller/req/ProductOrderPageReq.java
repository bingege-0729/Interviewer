package net.abing.controller.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 订单分页查询请求
 */
@Data
public class ProductOrderPageReq {

    /**
     * 页码（从1开始）
     */
    @Schema(description = "页码（从1开始）")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;

    /**
     * 订单状态（可选）：NEW/PAY/CANCEL
     */
    @Schema(description = "订单状态（可选）：NEW未支付, PAY已支付, CANCEL已取消")
    private String orderState;

    /**
     * 订单类型（可选）：PACKAGE_ORDER/BENEFIT_ORDER
     */
    @Schema(description = "订单类型（可选）：PACKAGE_ORDER/BENEFIT_ORDER")
    private String orderType;
}
