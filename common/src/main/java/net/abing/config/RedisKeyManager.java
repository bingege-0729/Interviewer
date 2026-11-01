package net.abing.config;

public class RedisKeyManager {

    /**
     * 验证码key，第一个 是类型，第二个是唯一标识
     */
    public static final String CHECK_CODE_KEY = "znoffer:code:%s:%s";


    /**
     * 微信access_token缓存,加入appID隔离
     * "wechat:access_token"
     */
    public static final String WECHAT_ACCESS_TOKEN = "znoffer:wechat:access_token:%s";

    /**
     * 微信access_token分布式锁key
     */
    public static final String WECHAT_ACCESS_TOKEN_LOCK = "znoffer:wechat:access_token:lock:%s";


    /**
     * 微信二维码缓存,%s就是场景id
     */
    public static final String WECHAT_SCENE_QRCODE = "znoffer:wechat:qrcode:status:%s";


    /**
     * 存储用户登录结果
     * %s就是场景id
     */
    public static final String WECHAT_LOGIN_RESULT = "znoffer:wechat:login:result:%s";
}
