package net.xdclass.service;

import jakarta.validation.Valid;
import net.xdclass.controller.req.BenefitOrderCreateReq;
import net.xdclass.controller.req.PackageOrderCreateReq;
import net.xdclass.controller.req.ProductOrderPageReq;
import net.xdclass.dto.ProductOrderDTO;
import net.xdclass.util.JsonData;
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
