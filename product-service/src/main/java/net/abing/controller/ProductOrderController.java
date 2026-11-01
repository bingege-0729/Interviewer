package net.abing.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.abing.controller.req.BenefitOrderCreateReq;
import net.abing.controller.req.PackageOrderCreateReq;
import net.abing.controller.req.ProductOrderPageReq;
import net.abing.dto.ProductOrderDTO;
import net.abing.service.ProductOrderService;
import net.abing.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order")
@Slf4j
public class ProductOrderController {

    @Autowired
    private ProductOrderService productOrderService;


    /**
     * 创建权益订单接口
     */
    @RequestMapping("/benefit/create")
    public JsonData createBenefitOrder(@Valid @RequestBody BenefitOrderCreateReq req){
        log.info("创建权益订单接口");
        return productOrderService.createBenefitOrder(req);
    }

    /**
     * 套餐订单下单接口
     */
    @RequestMapping("/package/create")
    public JsonData createPackageOrder(@Valid @RequestBody PackageOrderCreateReq req){
        log.info("创建套餐订单接口");
        return productOrderService.createPackageOrder(req);
    }

    /**
     * 查询订单支付状态接口
     * @param outTradeNo 商户订单号
     * @return 订单信息
     */
    @GetMapping("/status")
    public JsonData getOrderStatus(@RequestParam("outTradeNo") String outTradeNo) {
        log.info("查询订单支付状态, outTradeNo:{}", outTradeNo);
        ProductOrderDTO orderDTO = productOrderService.findByOutTradeNo(outTradeNo);
        return JsonData.buildSuccess(orderDTO);
    }

    /**
     * 分页查询当前用户订单列表接口
     */
    @PostMapping("/page")
    public JsonData page(@Valid @RequestBody ProductOrderPageReq req) {
        log.info("分页查询订单列表请求, page:{}, size:{}", req.getPage(), req.getSize());
        return JsonData.buildSuccess(productOrderService.page(req));
    }


    /**
     * 微信支付回调通知
     * 微信支付成功后回调此接口，更新订单状态
     *
     * <p>注意：此接口需在拦截器中配置为白名单，不需要登录验证</p>
     *
     * <p>微信支付回调规范：
     * <ul>
     *   <li>验签通过：返回 HTTP 200/204，无需返回应答报文</li>
     *   <li>验签不通过：返回 HTTP 5XX/4XX，并返回应答报文 {"code":"FAIL","message":"..."}</li>
     * </ul>
     */
    @Operation(summary = "微信支付回调", description = "微信支付Native支付结果通知，自动验签并更新订单状态")
    @PostMapping("/callback/wechat_pay")
    public ResponseEntity<String> wechatPayCallback(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signType,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serialNumber,
            @RequestBody String body) {

        log.info("收到微信支付回调，timestamp：{}，serial：{}", timestamp, serialNumber);
        log.info("收到微信支付回调，body：{}", body);

        return productOrderService.handleWechatPayCallback(
                timestamp, nonce, signature, signType, serialNumber, body);
    }


}
