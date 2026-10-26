package net.xdclass.feign;

import net.xdclass.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 商品服务Feign客户端
 *
 * 设计说明：
 * ai-service调用product-service查询商品、套餐和订单信息
 */
@FeignClient(name = "product-service", contextId = "ProductService")
public interface ProductFeign {

    /**
     * 查询可购买权益列表
     */
    @GetMapping("/api/v1/product/benefits")
    JsonData listBenefits();

    /**
     * 查询可购买套餐列表
     */
    @GetMapping("/api/v1/product/packages")
    JsonData listPackages();

    /**
     * 创建权益订单
     */
    @PostMapping("/api/v1/order/benefit/create")
    JsonData createBenefitOrder(@RequestBody Map<String, Object> req);

    /**
     * 创建套餐订单
     */
    @PostMapping("/api/v1/order/package/create")
    JsonData createPackageOrder(@RequestBody Map<String, Object> req);

    /**
     * 查询订单支付状态
     */
    @GetMapping("/api/v1/order/status")
    JsonData getOrderStatus(@RequestParam("outTradeNo") String outTradeNo);
}