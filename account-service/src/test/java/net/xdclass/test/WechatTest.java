package net.xdclass.test;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.OssClientManager;
import net.xdclass.service.OssService;
import net.xdclass.service.WechatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class WechatTest {


    @Autowired
    private WechatService wechatService;

    @Test
    public void testGetAccessToken(){

        String accessToken = wechatService.getAccessToken();
        log.info("access_token:{}", accessToken);

    }


}
