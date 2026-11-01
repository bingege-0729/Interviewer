package net.abing.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.WechatLoginConfig;
import net.abing.service.WechatService;
import net.abing.util.JsonData;
import net.abing.util.WechatUtil;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wechat")
public class WechatController {


    private final WechatLoginConfig wechatLoginConfig;


    private final WechatService wechatService;

    /**
     * 生成微信登陆二维码
     */
    @GetMapping("qrcode")
    @Operation(summary = "生成微信登陆二维码", description = "生成微信登陆二维码")
    public JsonData generateCode() {

        return wechatService.generateQrcode();
    }


    /**
     * 查询扫描状态
     */
    @GetMapping("login/result")
    @Operation(summary = "查询扫描状态", description = "查询扫描状态")
    public JsonData loginResult(@RequestParam("sceneId") String sceneId) {

        return wechatService.getLoginResult(sceneId);
    }


    /**
     * 处理微信事件推送
     */
    @PostMapping("/callback/verify")
    @ResponseBody
    public String handleEventCallback(HttpServletRequest request, HttpServletResponse response){

        try {
            //读取请求体
            String requestBody = WechatUtil.readRequestBody(request);
            log.info("微信服务器推送数据:{}", requestBody);

            //解析XML
            Map<String, String> eventData = WechatUtil.parseXmlData(requestBody);
            String eventType = eventData.get("Event");
            String openId = eventData.get("FromUserName");
            String sceneId = eventData.get("EventKey");

            log.info("微信扫码事件,openId:{},sceneId:{}", openId, sceneId);


            //路由事件类型
            switch (eventType){
                case "SCAN":
                    log.info("处理扫码事件");
                    wechatService.handleEventCallback(sceneId,openId);
                    break;
                case "subscribe":
                    log.info("处理关注事件");
                    if(sceneId!= null && sceneId.startsWith("qrscene_")){
                        sceneId = sceneId.substring(8);
                        wechatService.handleEventCallback(sceneId,openId);
                    }
                    break;
                case "unsubscribe":
                    log.info("处理取消关注事件");
                    break;
                default:
                    log.info("处理未知事件");
                    break;
            }

            return "success";

        }catch (Exception e){
            log.error("微信服务器验证异常");
            return "error";
        }


    }




    /**
     * 微信服务器验证接口
     */
    @GetMapping("/callback/verify")
    @Operation(
            summary = "微信服务器验证接口",
            description = "微信服务器验证接口"
    )
    public String callbackVerify(@RequestParam("signature") String signature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce,
                                 @RequestParam("echostr") String echostr) {

        log.info("微信服务器验证接口,signature:{},timestamp:{},nonce:{},echostr:{}", signature, timestamp, nonce, echostr);

        if(verifySignature(signature, timestamp, nonce)){
            log.info("微信服务器验证成功");
            return echostr;
        }else {
            log.error("微信服务器验证失败");
            return "error";
        }




    }

    /**
     * 验证微信服务器
     * @param signature
     * @param timestamp
     * @param nonce
     * @return
     */
    private boolean verifySignature(String signature, String timestamp, String nonce) {

        try {
            // 1、参数排序
            String[] paramArr = new String[]{wechatLoginConfig.getToken(), timestamp, nonce};
            Arrays.sort(paramArr);

            // 2 把3个参数拼接字符串
            String paramStr = String.join("", paramArr);

            // 3 sha1加密
            MessageDigest instance = MessageDigest.getInstance("SHA-1");
            byte[] digest = instance.digest(paramStr.getBytes());

            // 4. 将加密后的字节数组转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // 5 将签名和微信发送的进行对比
            boolean equals = signature.equals(hexString.toString());

            log.info("微信服务器验证结果:{}", equals);

            return equals;
        }catch (Exception e){
            log.error("微信服务器验证失败:{}", e);
            return false;
        }
    }

}
