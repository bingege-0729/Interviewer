package net.xdclass.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.InterviewEventMQConfig;
import net.xdclass.dto.InterviewCreateMessageDTO;
import net.xdclass.enums.BenefitEnum;
import net.xdclass.strategy.BenefitMessageStrategy;
import net.xdclass.strategy.BenefitMessageStrategyFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 面试创建消息发送策略
 *
 * 设计说明：
 * - 策略实现在ai-service中
 * - 负责发送面试创建业务MQ消息
 * - 支持所有面试类型（专项面试、简历综合面）
 * - 消息由ai-service的消费者处理
 *
 * @author 小滴课堂
 */
@Component
@Slf4j
public class InterviewCreateMessageStrategy implements BenefitMessageStrategy {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public void sendBusinessMessage(String messageId, String businessId, Long accountId) {

        long interviewId = Long.parseLong(businessId);

        //构建对应的消息DTO
        InterviewCreateMessageDTO createMessageDTO = InterviewCreateMessageDTO.builder()
                .interviewId(interviewId)
                .accountId(accountId)
                .messageId(messageId)
                .build();

        //发送消息
        rabbitTemplate.convertAndSend(InterviewEventMQConfig.INTERVIEW_EVENT_EXCHANGE,
                InterviewEventMQConfig.INTERVIEW_CREATE_ROUTING_KEY, createMessageDTO);

        log.info("发送面试创建消息成功, createMessageDTO:{}", createMessageDTO);

    }

    @Override
    public String getStrategyType() {
        return BenefitEnum.INTERVIEW.name();
    }
}
