package net.xdclass.test;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.OssClientManager;
import net.xdclass.service.OssService;
import net.xdclass.util.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class OssManagerTest {

    @Autowired
    private OssClientManager ossClientManager;


    @Autowired
    private OssService ossService;


    @Test
    public void testDeleteFile(){
        String fileUrl = "https://test-file-pub.znoffer.com/iShot_2026-01-22_13.56.46.png";
        //跟进URL解析存储桶
        OssClientManager.BucketClient bucketClient = ossClientManager.resolveByUrl(fileUrl);
        //提取fileKey
        String fileKey = ossService.extractFileKey(fileUrl);
        //使用客户端删除
        bucketClient.getClient().deleteObject(bucketClient.getBucketName(),fileKey);
        log.info("删除成功");
    }


    @Test
    public void testGeneratePreviewUrl(){
        String fileUrl = "https://test-file-pri.znoffer.com/test.pdf";

        //解析bucket
        OssClientManager.BucketClient bucketClient = ossClientManager.resolveByUrl(fileUrl);

        if(bucketClient.isPublicBucket()){
            log.info("公开存储桶，直接返回URL");
        }

        String previewUrl = ossService.generatePreviewUrl(fileUrl);
        log.info("预览URL：{}",previewUrl);


    }

}
