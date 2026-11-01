package net.abing.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import net.abing.config.InterviewEventMQConfig;
import net.abing.dto.ResumeAnalyseMessageDTO;
import net.abing.enums.BenefitEnum;
import net.abing.strategy.BenefitMessageStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 简历分析消息发送策略
 *
 * 设计说明：
 * - 策略实现在ai-service中
 * - 负责发送简历分析业务MQ消息
 * - 消息由ai-service的消费者处理
 *
 */
@Component
@Slf4j
public class ResumeAnalyseMessageStrategy implements BenefitMessageStrategy {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public void sendBusinessMessage(String messageId, String businessId, Long accountId) {
        Long resumeId = Long.parseLong(businessId);

        //构建对应的消息DTO
        ResumeAnalyseMessageDTO resumeAnalyseMessageDTO = ResumeAnalyseMessageDTO.builder()
                .resumeId(resumeId)
                .accountId(accountId)
                .messageId(messageId)
                .build();

        rabbitTemplate.convertAndSend(InterviewEventMQConfig.INTERVIEW_EVENT_EXCHANGE,
                InterviewEventMQConfig.RESUME_ANALYSE_ROUTING_KEY, resumeAnalyseMessageDTO);

        log.info("发送简历分析消息成功, resumeAnalyseMessageDTO:{}", resumeAnalyseMessageDTO);


    }

    @Override
    public String getStrategyType() {
        return BenefitEnum.RESUME_ANALYSE.name();
    }
}
