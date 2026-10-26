package net.xdclass.controller;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import net.xdclass.config.WechatPayConfig;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pay/test")
public class WechaPayTestController {


    @Autowired
    private WechatPayConfig wechatPayConfig;


    /**
     * 微信支付native测试
     */
    @GetMapping("/native")
    public JsonData nativePay(@RequestParam("amount") int amount) {

        Config config =
                new RSAPublicKeyConfig.Builder()
                        .merchantId(wechatPayConfig.getMchId())
                        .privateKeyFromPath(wechatPayConfig.getPrivateKeyAbsolutePath())
                        .publicKeyFromPath(wechatPayConfig.getPublicKeyAbsolutePath())
                        .publicKeyId(wechatPayConfig.getPublicKeyId())
                        .merchantSerialNumber(wechatPayConfig.getCertSerialNo())
                        .apiV3Key(wechatPayConfig.getApiV3Key())
                        .build();
        // 构建service
        NativePayService service = new NativePayService.Builder().config(config).build();
        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        PrepayRequest request = new PrepayRequest();
        Amount reAmount = new Amount();
        reAmount.setTotal(amount);
        request.setAmount(reAmount);
        request.setAppid(wechatPayConfig.getAppId());
        request.setMchid(wechatPayConfig.getMchId());
        request.setDescription("测试商品标题");
        request.setNotifyUrl(wechatPayConfig.getNotifyUrl());
        String outTradeNo = CommonUtil.generateUUID();
        request.setOutTradeNo(outTradeNo);
        // 调用下单方法，得到应答
        PrepayResponse response = service.prepay(request);
        // 使用微信扫描 code_url 对应的二维码，即可体验Native支付
        System.out.println(response.getCodeUrl());

        Map<String,Object> result = new HashMap<>();
        result.put("code_url",response.getCodeUrl());
        result.put("amount",amount);
        result.put("out_trade_no",outTradeNo);

        return JsonData.buildSuccess(result);
    }



}
