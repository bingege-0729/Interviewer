package net.xdclass.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.config.InterviewEventMQConfig;
import net.xdclass.dto.ResumeAnalyseMessageDTO;
import net.xdclass.service.ResumeAnalyseService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RabbitListener(queues = InterviewEventMQConfig.RESUME_ANALYSE_QUEUE)
public class ResumeAnalyseListener {

    private final ResumeAnalyseService resumeAnalyseService;


    /**
     * 处理简历分析任务
     */
    @RabbitHandler
    public void handleResumeAnalyse(ResumeAnalyseMessageDTO messageDTO, Message amqpMessage, Channel channel) {

        Long resumeId = messageDTO.getResumeId();
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        log.info("消费者收到简历分析的任务, resumeId: {}", resumeId);
        try {
            boolean success = resumeAnalyseService.processResumeAnalyse(messageDTO);
            if (success) {
                channel.basicAck(deliveryTag, false);
                log.info("简历分析任务处理成功, resumeId: {}", resumeId);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.info("简历分析任务处理失败, resumeId: {}", resumeId);
            }

        }catch (Exception e){
            log.error("处理简历分析任务失败, resumeId: {}", resumeId, e);
            try {
                channel.basicNack(deliveryTag, false,false);
            }catch (Exception e1){
                log.error("消息拒绝失败");
            }
        }


    }


}
