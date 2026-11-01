package net.abing.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.BenefitGrantMQConfig;
import net.abing.dto.BenefitGrantMessage;
import net.abing.service.AccountBenefitService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RabbitListener(queues = BenefitGrantMQConfig.BENEFIT_GRANT_QUEUE)
public class BenefitGrantMQListener {

    private final AccountBenefitService accountBenefitService;


    @RabbitHandler
    public void benefitGrantHandler(BenefitGrantMessage message, Message amqpMessage, Channel channel) {

        //打印详细日志
        log.info("收到消息：{}",message);
        try {
            accountBenefitService.grantBenefit(message);
            log.info("处理完成，确认消息");
            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            // 更新日志错误输出格式
            log.error("处理消息异常：message={}",message,e);
            try {
                channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(),false,false);
            }catch (Exception ex){
                log.error("消息拒绝失败");
            }
        }
    }


}
