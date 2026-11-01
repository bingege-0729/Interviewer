package net.abing.listener;


import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.ProductOrderMQConfig;
import net.abing.dto.ProductOrderDTO;
import net.abing.service.ProductOrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RabbitListener(queues = ProductOrderMQConfig.orderCloseQueue)
public class ProductOrderMQListener {

    @Autowired
    private ProductOrderService productOrderService;



    @RabbitHandler
    public void productOrderHandler(ProductOrderDTO productOrderDTO, Message message, Channel channel){

        log.info("收到订单关闭消息,订单号：{}",productOrderDTO.getOutTradeNo());

        try {
            productOrderService.handleTimeoutOrder(productOrderDTO);

            log.info("订单关闭成功,订单号：{}",productOrderDTO.getOutTradeNo());

            // 确认消息 手动ACK
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

        }catch (Exception e){
            log.error("订单关闭异常,订单号：{}",productOrderDTO.getOutTradeNo());
            try {
                //拒绝消息，不重新入队
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            }catch (Exception ex){
                log.error("消息拒绝失败",ex);
            }
        }
    }

}
