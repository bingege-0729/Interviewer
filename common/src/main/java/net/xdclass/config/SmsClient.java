package net.xdclass.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dto.SmsDTO;
import net.xdclass.util.HttpUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SmsClient {

    @Resource
    private SmsConfig smsConfig;


    public String sendSms(SmsDTO smsDTO){
        String url = smsConfig.getHost() + smsConfig.getPath();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + smsConfig.getAppCode());

        Map<String, Object> querys = new HashMap<>();
        querys.put("mobile", smsDTO.getMobile());
        querys.put("param", smsDTO.getParam());

        //smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html
        querys.put("smsSignId", smsConfig.getSmsSignId());
        querys.put("templateId", smsConfig.getTemplateId());

        String resp = HttpUtil.postForm(url, headers, querys);
        log.info("[发送短信] req: {}, resp: {}", smsDTO, resp);
        return resp;

    }


}
