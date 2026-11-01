package net.abing.service;


import net.abing.util.JsonData;

public interface WechatService {

     String getAccessToken();

    JsonData generateQrcode();

    JsonData getLoginResult(String sceneId);

    JsonData handleEventCallback(String sceneId, String openId);
}
