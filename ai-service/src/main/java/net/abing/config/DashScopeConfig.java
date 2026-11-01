package net.abing.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DashScopeConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 配置DashScope ChatModel
     *
     */
    @Bean
    public DashScopeApi dashScopeApi(){
        return  DashScopeApi.builder().apiKey(apiKey).build();
    }


    @Bean
    public ChatModel chatModel(DashScopeApi dashScopeApi){
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }


    @Bean
    public ChatClient chatClient(ChatModel chatModel){
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


}
