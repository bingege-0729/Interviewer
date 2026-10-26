package net.xdclass.config;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign请求拦截器配置
 * 自动将当前请求的token透传到Feign调用的目标服务
 * 解决微服务间调用时用户身份丢失的问题
 */
@Slf4j
@Configuration
public class FeignRequestInterceptor {

    @Bean
    public RequestInterceptor tokenRelayInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String token = request.getHeader("token");
                    if (token != null && !token.isEmpty()) {
                        template.header("token", token);
                    }
                }
            }
        };
    }
}