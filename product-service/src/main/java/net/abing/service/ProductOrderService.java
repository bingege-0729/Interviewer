package net.abing.service;

import jakarta.validation.Valid;
import net.abing.controller.req.BenefitOrderCreateReq;
import net.abing.controller.req.PackageOrderCreateReq;
import net.abing.controller.req.ProductOrderPageReq;
import net.abing.dto.ProductOrderDTO;
import net.abing.util.JsonData;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ProductOrderService {
    JsonData createBenefitOrder(@Valid BenefitOrderCreateReq req);

    JsonData createPackageOrder(@Valid PackageOrderCreateReq req);

    /**
     * 根据商户订单号查询订单
     * @param outTradeNo 商户订单号
     * @return 订单DTO
     */
    ProductOrderDTO findByOutTradeNo(String outTradeNo);

    /**
     * 分页查询当前用户的订单列表
     * @param req 分页查询请求
     * @return 分页结果
     */
    Map<String, Object> page(ProductOrderPageReq req);

    /**
     * 微信支付回调
     * @param timestamp
     * @param nonce
     * @param signature
     * @param signType
     * @param serialNumber
     * @param body
     * @return
     */
    ResponseEntity<String> handleWechatPayCallback(String timestamp, String nonce, String signature, String signType, String serialNumber, String body);

    void handleTimeoutOrder(ProductOrderDTO productOrderDTO);
}
