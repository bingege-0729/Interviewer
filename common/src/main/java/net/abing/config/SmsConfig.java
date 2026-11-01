package net.abing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sms")
public class SmsConfig {

    private String host = "https://gyytz.market.alicloudapi.com";

    private String path = "/sms/smsSend";

    private String appCode;

    private String smsSignId;

    private String templateId;
}
