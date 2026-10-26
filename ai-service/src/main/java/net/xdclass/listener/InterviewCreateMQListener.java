package net.xdclass.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.InterviewEventMQConfig;
import net.xdclass.dto.InterviewCreateMessageDTO;
import net.xdclass.service.InterviewService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
@RequiredArgsConstructor
@Slf4j
@RabbitListener(queues = InterviewEventMQConfig.INTERVIEW_CREATE_QUEUE)
public class InterviewCreateMQListener {


    private final InterviewService interviewService;


    @RabbitHandler
    public void handleMessage(InterviewCreateMessageDTO messageDTO,
                              Message amqpMessage, Channel channel) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            log.info("收到面试创建消息，面试ID：{}", messageDTO.getInterviewId());

            // 调用Service层生成面试内容
            interviewService.generateInterview(messageDTO);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理面试创建消息失败", e);
            try {
                // 不重回队列，避免死循环
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("消息NACK失败", ex);
            }
        }
    }


}
