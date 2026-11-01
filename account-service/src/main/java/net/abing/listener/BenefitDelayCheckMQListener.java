package net.abing.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.BenefitDelayCheckMQConfig;
import net.abing.dto.BenefitDelayCheckMessageDTO;
import net.abing.service.BenefitTaskService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RabbitListener(queues = BenefitDelayCheckMQConfig.BENEFIT_DELAY_CHECK_CONSUME_QUEUE)
public class BenefitDelayCheckMQListener {

    private final BenefitTaskService benefitTaskService;



    @RabbitHandler
    public void handleDelayCheck(BenefitDelayCheckMessageDTO messageDTO, Message message, Channel channel){

        log.info("监听到延迟检查消息, messageId={}, businessId={}, checkLevel={}",
                messageDTO.getMessageId(), messageDTO.getBusinessId(), messageDTO.getCheckLevel());

        try {

            //执行检查逻辑
            benefitTaskService.checkAndCompensate(messageDTO.getMessageId(),messageDTO.getCheckLevel());

            //手动Ack
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            log.info("延迟消息消费成功:{}",messageDTO);

        }catch (Exception e){
            log.error("处理延迟检查消息失败，messageId={}", messageDTO.getMessageId(), e);
            try {
                //拒绝消息，不重新入队
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            }catch (Exception ex){
                log.error("处理延迟检查消息失败，messageId={}", messageDTO.getMessageId(), ex);
            }
        }

    }



}
