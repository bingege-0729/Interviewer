package net.xdclass.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI面试业务MQ配置
 * 用于ai-service发送业务消息
 *
 * 设计说明：
 * - ai-service负责发送业务MQ消息
 * - 简历分析消息、面试创建消息等
 *
 * @author 小滴课堂
 */
@Configuration
public class InterviewEventMQConfig {


    // ============ 交换机 ============
    public static final String INTERVIEW_EVENT_EXCHANGE = "interview.event.exchange";

    // ============ 简历分析队列 ============
    public static final String RESUME_ANALYSE_QUEUE = "resume.analyse.queue";
    public static final String RESUME_ANALYSE_ROUTING_KEY = "resume.analyse";

    // ============ 面试创建队列 ============
    public static final String INTERVIEW_CREATE_QUEUE = "interview.create.queue";
    public static final String INTERVIEW_CREATE_ROUTING_KEY = "interview.create";


    /**
     * 配置交换机
     */
    @Bean
    public TopicExchange interviewEventExchange() {
        return new TopicExchange(INTERVIEW_EVENT_EXCHANGE,true,false);
    }


    /**
     * 简历分析的队列
     */
    @Bean
    public Queue resumeAnalyseQueue() {
        return new Queue(RESUME_ANALYSE_QUEUE,true);
    }


    /**
     * 面试创建的队列
     */
    @Bean
    public Queue interviewCreateQueue() {
        return new Queue(INTERVIEW_CREATE_QUEUE,true);
    }


    /**
     * 绑定交换机:简历分析队列和交换机绑定
     */
    @Bean
    public Binding resumeAnalyseBinding(){
        return BindingBuilder.bind(resumeAnalyseQueue())
                .to(interviewEventExchange())
                .with(RESUME_ANALYSE_ROUTING_KEY);
    }


    /**
     * 绑定交换机:面试创建队列和交换机绑定
     */
    @Bean
    public Binding interviewCreateBinding(){
        return BindingBuilder.bind(interviewCreateQueue())
                .to(interviewEventExchange())
                .with(INTERVIEW_CREATE_ROUTING_KEY);
    }



}



