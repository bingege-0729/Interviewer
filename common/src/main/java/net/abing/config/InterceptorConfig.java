package net.abing.config;

import lombok.extern.slf4j.Slf4j;
import net.abing.interceptor.AccountLoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new AccountLoginInterceptor())
                //拦截的路径
                .addPathPatterns("/api/*/**")

                //不拦截的路径
                .excludePathPatterns(
                        "/api/v1/account/login",
                        "/api/v1/account/captcha",
                        "/api/v1/account/send_check_code",
                        "/api/v1/banner/*",
                        // 微信登录相关接口
                        "/api/v1/wechat/qrcode",
                        "/api/v1/wechat/qrcode/status",
                        "/api/v1/wechat/login/result",
                        "/api/v1/wechat/auth/url",
                        "/api/v1/wechat/auth/callback",
                        // 微信回调接口
                        "/api/v1/wechat/callback/**",
                        "/api/v1/wechat/scan/callback",
                        // 产品相关接口（不需要登录）
                        "/api/v1/product/packages",
                        "/api/v1/product/benefits",
                        "/api/v1/product/package/detail",
                        "/api/v1/product/benefit/detail",
                        // 支付回调接口（不需要登录）
                        "/api/v1/order/callback/wechat_pay",

                        //内部服务调用端口，不拦截（可以由网关层面保护，不对外请求）
                        "/api/v1/account/benefit/checkAndDeduct",
                        "/api/v1/account/benefit/task/updateStatus",
                        "/api/v1/account/benefit/list",

                        "/api/v1/order/callback/alipay",
                        // 静态资源
                        "/favicon.ico",
                        "/error",

                        //测试路径
                        "/api/v1/pay/test/native",
                        "/api/v1/ai/*/**",
                        "/api/v1/graph-chat/*/**",
                        "/api/v1/graph/*/**"
                        )
                .order(1); // 优先级
    }
}
