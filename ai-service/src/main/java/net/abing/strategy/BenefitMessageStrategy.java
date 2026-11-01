package net.abing.strategy;

/**
 * 权益消息发送的策略接口
 */
public interface BenefitMessageStrategy {


    /**
     * 发送业务消息
     * @param messageId
     * @param businessId
     * @param accountId
     */
    void sendBusinessMessage(String messageId,String businessId,Long accountId);


    /**
     * 获取策略类型（就是权益编码）
     * @return
     */
    String getStrategyType();
}
