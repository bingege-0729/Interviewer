package net.abing.test;

import lombok.extern.slf4j.Slf4j;
import net.abing.util.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
public class SmsTest {

    @Test
    public void testSendSms() {
        log.info("开始测试发送短信");
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String url = host + path;
        String appcode = "08d1630e01964495b5b2e07c2f87605a";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + appcode);

        Map<String, Object> querys = new HashMap<>();
        querys.put("mobile", "13113777337");
        querys.put("param", "**code**:12345,**minute**:5");

        //smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html
        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");

        String response = HttpUtil.postForm(url, headers, querys);
        log.info("响应内容: {}", response);
    }


}
