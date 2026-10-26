package net.xdclass.service;


import net.xdclass.util.JsonData;

public interface WechatService {

     String getAccessToken();

    JsonData generateQrcode();

    JsonData getLoginResult(String sceneId);

    JsonData handleEventCallback(String sceneId, String openId);
}
