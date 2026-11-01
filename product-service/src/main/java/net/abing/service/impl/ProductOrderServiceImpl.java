package net.abing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.config.BenefitGrantMQConfig;
import net.abing.config.ProductOrderMQConfig;
import net.abing.controller.req.BenefitOrderCreateReq;
import net.abing.controller.req.PackageOrderCreateReq;
import net.abing.controller.req.ProductOrderPageReq;
import net.abing.dto.AccountDTO;
import net.abing.dto.BenefitGrantMessage;
import net.abing.dto.PayInfoDTO;
import net.abing.dto.ProductOrderDTO;
import net.abing.enums.OrderStateEnum;
import net.abing.enums.OrderTypeEnum;
import net.abing.enums.PaymentEnum;
import net.abing.enums.StatusEnum;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.mapper.BenefitMapper;
import net.abing.mapper.PackageBenefitMapper;
import net.abing.mapper.ProductOrderMapper;
import net.abing.mapper.ProductPackageMapper;
import net.abing.model.BenefitDO;
import net.abing.model.PackageBenefitDO;
import net.abing.model.ProductOrderDO;
import net.abing.model.ProductPackageDO;
import net.abing.service.ProductOrderService;
import net.abing.util.CommonUtil;
import net.abing.util.JsonData;
import net.abing.util.SpringBeanUtil;
import net.abing.util.WechatPayUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductOrderServiceImpl implements ProductOrderService {

    private final ProductOrderMapper productOrderMapper;

    private final ProductPackageMapper productPackageMapper;

    private final BenefitMapper benefitMapper;

    private final WechatPayUtil wechatPayUtil;

    private final ProductOrderMQConfig productOrderMQConfig;

    private final RabbitTemplate rabbitTemplate;


    private final PackageBenefitMapper packageBenefitMapper;

    /**
     * 创建权益订单
     * @param req
     * @return
     */
    @Override
    public JsonData createBenefitOrder(BenefitOrderCreateReq req) {

        BenefitDO benefitDO = benefitMapper.selectById(req.getBenefitId());
        if(benefitDO == null){
            return JsonData.buildError("权益不存在");
        }
        //判断状态
        if(benefitDO.getStatus().equals(StatusEnum.OFF.name())){
            return JsonData.buildError("权益已下架");
        }

        //统计订单金额
        BigDecimal payAmount = benefitDO.getUnitPrice().multiply(new BigDecimal(req.getPurchaseCount()));

        //构建订单对象
        ProductOrderDO productOrderDO = ProductOrderDO.builder()
                .outTradeNo("BFT" + CommonUtil.getCurrentTimestamp() + CommonUtil.generateStringNumRandom(6))
                .accountId(getCurrentAccountId())
                .orderType(OrderTypeEnum.BENEFIT_ORDER.name())
                .orderState(OrderStateEnum.NEW.name())
                .title(benefitDO.getName())
                .benefitId(benefitDO.getId())
                .purchaseCount(req.getPurchaseCount())
                .unitPrice(benefitDO.getUnitPrice())
                .discount(req.getDiscount())
                .payAmount(payAmount)
                .payType(PaymentEnum.valueOf(req.getPayType()).name())
                .build();
        productOrderMapper.insert(productOrderDO);
        log.info("创建订单成功:{}",productOrderDO);
        return  handlePayment(productOrderDO,req.getPayType());
    }

    @Override
    public JsonData createPackageOrder(PackageOrderCreateReq req) {

        ProductPackageDO productPackageDO = productPackageMapper.selectById(req.getPackageId());
        if(productPackageDO == null){
            return JsonData.buildError("套餐不存在");
        }
        //判断状态
        if(productPackageDO.getStatus().equals(StatusEnum.OFF.name())){
            return JsonData.buildError("套餐已下架");
        }

        //构建订单对象
        ProductOrderDO productOrderDO = ProductOrderDO.builder()
                .outTradeNo("PKG" + CommonUtil.getCurrentTimestamp() + CommonUtil.generateStringNumRandom(6))
                .accountId(getCurrentAccountId())
                .orderType(OrderTypeEnum.PACKAGE_ORDER.name())
                .orderState(OrderStateEnum.NEW.name())
                .title(productPackageDO.getName())
                .productPackageId(productPackageDO.getId())
                .purchaseCount(1)
                .unitPrice(productPackageDO.getPrice())
                .discount(req.getDiscount())
                .payAmount(productPackageDO.getPrice())
                .payType(PaymentEnum.valueOf(req.getPayType()).name())
                .build();
        productOrderMapper.insert(productOrderDO);
        log.info("创建订单成功:{}",productOrderDO);
        return  handlePayment(productOrderDO,req.getPayType());
    }


    /**
     * 根据支付方式路由到对应的支付逻辑
     */
    private JsonData handlePayment(ProductOrderDO orderDO,String payType){
        //发送MQ消息
        ProductOrderDTO productOrderDTO = SpringBeanUtil.copyProperties(orderDO, ProductOrderDTO.class);
        rabbitTemplate.convertAndSend(productOrderMQConfig.getOrderEventExchange(),
                productOrderMQConfig.getOrderCloseDelayRoutingKey(),productOrderDTO);

        if(PaymentEnum.WECHAT_PAY.name().equals(payType)){
            //微信支付
            return doWechatNativePay(orderDO);
        }else if(PaymentEnum.ALI_PAY.name().equals(payType)){
            return JsonData.buildError("支付宝支付暂时未实现");
        }
        return JsonData.buildError("不支持的支付方式");
    }

    /**
     * 发起Native支付,构建支付信息
     * @param orderDO
     * @param description
     * @return
     */
    private JsonData doWechatNativePay(ProductOrderDO orderDO) {
        long payAmountFen = orderDO.getPayAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String codeUrl = wechatPayUtil.createNativePayOrder(orderDO.getOutTradeNo(), payAmountFen, orderDO.getTitle());
        if(StringUtils.isBlank(codeUrl)){
            log.error("微信支付下单失败");
            return JsonData.buildError("微信支付下单失败");
        }
        PayInfoDTO payInfoDTO = PayInfoDTO.builder()
                .outTradeNo(orderDO.getOutTradeNo())
                .payAmount(payAmountFen)
                .payType(PaymentEnum.WECHAT_PAY.name())
                .codeUrl(codeUrl).build();

        log.info("微信支付下单成功，返回参数:{}",payInfoDTO);
        return JsonData.buildSuccess(payInfoDTO);
    }


    private Long getCurrentAccountId(){

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();
        return accountDTO.getId();
    }

    @Override
    public ProductOrderDTO findByOutTradeNo(String outTradeNo) {
        log.info("查询订单, outTradeNo:{}", outTradeNo);
        
        ProductOrderDO orderDO = productOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductOrderDO>()
                        .eq(ProductOrderDO::getOutTradeNo, outTradeNo)
                        .eq(ProductOrderDO::getAccountId, getCurrentAccountId())
        );
        
        if (orderDO == null) {
            log.warn("订单不存在, outTradeNo:{}", outTradeNo);
            return null;
        }
        
        log.info("查询订单成功, outTradeNo:{}, orderState:{}", outTradeNo, orderDO.getOrderState());
        return SpringBeanUtil.copyProperties(orderDO, ProductOrderDTO.class);
    }

    @Override
    public Map<String, Object> page(ProductOrderPageReq req) {
        Long accountId = getCurrentAccountId();
        log.info("分页查询订单列表, accountId:{}, page:{}, size:{}", accountId, req.getPage(), req.getSize());

        // 1. 构建查询条件
        LambdaQueryWrapper<ProductOrderDO> queryWrapper = new LambdaQueryWrapper<ProductOrderDO>()
                .eq(ProductOrderDO::getAccountId, accountId)
                .eq(StringUtils.isNotBlank(req.getOrderState()), ProductOrderDO::getOrderState, req.getOrderState())
                .eq(StringUtils.isNotBlank(req.getOrderType()), ProductOrderDO::getOrderType, req.getOrderType())
                .orderByDesc(ProductOrderDO::getGmtCreate);

        // 2. 分页查询
        Page<ProductOrderDO> page = new Page<>(req.getPage(), req.getSize());
        Page<ProductOrderDO> resultPage = productOrderMapper.selectPage(page, queryWrapper);

        // 3. DO转DTO并构建结果
        List<ProductOrderDTO> dtoList = SpringBeanUtil.copyProperties(resultPage.getRecords(), ProductOrderDTO.class);

        Map<String, Object> result = CommonUtil.convertToPageMap(resultPage, dtoList);
        log.info("分页查询订单列表成功, accountId:{}, total:{}", accountId, resultPage.getTotal());
        return result;
    }
    private static final String WECHAT_CALLBACK_SUCCESS = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";

    @Override
    public ResponseEntity<String> handleWechatPayCallback(String timestamp, String nonce, String signature, String signType, String serialNumber, String body) {

        Transaction transaction = wechatPayUtil.parseCallbackNotification(timestamp, nonce, signature, signType, serialNumber, body);
        if(transaction == null){
            log.error("解析微信回调异常");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildFailResponse("解析微信回调异常"));
        }

        String outTradeNo = transaction.getOutTradeNo();
        Transaction.TradeStateEnum tradeState = transaction.getTradeState();
        log.info("微信回调, outTradeNo:{}, tradeState:{}", outTradeNo, tradeState);
        ProductOrderDO productOrderDO = selectByOutTradeNo(outTradeNo);
        if (productOrderDO == null) {
            log.error("订单不存在, outTradeNo:{}", outTradeNo);
            //验证微信回调通过，不返回任何内容
            //return ResponseEntity.ok().build();
            return ResponseEntity.ok(WECHAT_CALLBACK_SUCCESS);
        }

        if (Transaction.TradeStateEnum.SUCCESS.equals(tradeState)) {

            //检查数据库是否已经更新
            if(productOrderDO.getOrderState().equals(OrderStateEnum.PAY.name())){
                log.warn("订单已支付, outTradeNo:{}", outTradeNo);
                //return ResponseEntity.ok().build();
                return ResponseEntity.ok(WECHAT_CALLBACK_SUCCESS);

            }
            //更新数据库
            Integer updated = updateOrderState(outTradeNo,transaction.getTransactionId(),OrderStateEnum.PAY.name());
            if(updated > 0){
                processOrderBenefits(productOrderDO.getOutTradeNo());
                log.info("更新订单成功, outTradeNo:{}", outTradeNo);
            }else {
                log.warn("更新订单失败, outTradeNo:{}", outTradeNo);
            }

        } else if (!OrderStateEnum.PAY.name().equals(productOrderDO.getOrderState())){
            Integer updated = updateOrderState(outTradeNo,transaction.getTransactionId(),OrderStateEnum.CANCEL.name());
            if(updated<1){
                log.info("订单未支付成功, outTradeNo:{}", outTradeNo);
            }else {
                log.warn("更新订单失败，冲突或者不存在, outTradeNo:{}", outTradeNo);
            }
        }

        //return ResponseEntity.ok().build();
        return ResponseEntity.ok(WECHAT_CALLBACK_SUCCESS);

    }

    @Override
    public void handleTimeoutOrder(ProductOrderDTO productOrderDTO) {
        String outTradeNo = productOrderDTO.getOutTradeNo();
        log.info("处理订单超时, outTradeNo:{}", outTradeNo);
        try {
            //查询订单
            ProductOrderDO productOrderDO = selectByOutTradeNo(outTradeNo);
            if(productOrderDO == null){
                log.warn("订单不存在, outTradeNo:{}", outTradeNo);
                return;
            }

            //判断状态是否已经支付
            if(OrderStateEnum.PAY.name().equals(productOrderDO.getOrderState())){
                log.warn("订单已支付, outTradeNo:{}", outTradeNo);
                return;
            }
            //已经取消的订单也直接返回
            if(OrderStateEnum.CANCEL.name().equals(productOrderDO.getOrderState())){
                log.warn("订单已取消, outTradeNo:{}", outTradeNo);
                return;
            }

            //查询第三方支付的订单状态
            if(PaymentEnum.WECHAT_PAY.name().equals(productOrderDO.getPayType())){
                handleWechatTimeoutOrder(productOrderDO);
            }

        }catch (Exception e){
            log.error("处理订单超时异常, outTradeNo:{}", outTradeNo, e);
        }

    }

    private void handleWechatTimeoutOrder(ProductOrderDO productOrderDO) {
        String outTradeNo = productOrderDO.getOutTradeNo();
        Transaction transaction = wechatPayUtil.queryOrderByOutTradeNo(outTradeNo);
        Transaction.TradeStateEnum tradeState = transaction.getTradeState();
        log.info("微信订单支付状态:{}", tradeState);

        if(Transaction.TradeStateEnum.SUCCESS.equals(tradeState)){
            log.warn("回调异常补偿机制,第三方已经支付，本地没更新，订单号{}",outTradeNo);
            int updated = updateOrderState(outTradeNo,transaction.getTransactionId(),OrderStateEnum.PAY.name());
            if(updated > 0){
                processOrderBenefits(productOrderDO.getOutTradeNo());
                log.info("更新订单成功, outTradeNo:{}", outTradeNo);
            }else {
                log.warn("更新订单失败，冲突或者不存在, outTradeNo:{}", outTradeNo);
            }
        } else if(Transaction.TradeStateEnum.USERPAYING.equals(tradeState)){
            log.warn("订单支付中, outTradeNo:{}", outTradeNo);
        } else {
            log.warn("订单未支付,取消订单 outTradeNo:{},tradeState{}", outTradeNo,tradeState);
            cancelOrder(productOrderDO);
        }

    }

    private void cancelOrder(ProductOrderDO productOrderDO) {
        wechatPayUtil.closeOrder(productOrderDO.getOutTradeNo());
        productOrderDO.setOrderState(OrderStateEnum.CANCEL.name());
        productOrderMapper.updateById(productOrderDO);
        log.info("取消订单成功, outTradeNo:{}", productOrderDO.getOutTradeNo());
    }


    /**
     * 更新订单状态为指定状态，带幂等检查和数据库级 CAS 保障
     * 1、第一次推送
     * 2、第二次推送 本身数据库订单是支付成功，幂等检查
     * @param outTradeNo
     * @param transactionId
     * @param newState
     * @return
     */
    private Integer updateOrderState(String outTradeNo, String transactionId, String newState) {
        ProductOrderDO productOrderDO = selectByOutTradeNo(outTradeNo);
        if(productOrderDO == null){
            log.info("订单不存在, outTradeNo:{}", outTradeNo);
            return 0;
        }
        if( newState.equals(productOrderDO.getOrderState())){
            log.info("订单{} 已经是 {},跳过", outTradeNo,newState);
            return 0;
        }
        //数据库级的CAS
        int updated = productOrderMapper.update(null,
                new LambdaUpdateWrapper<ProductOrderDO>()
                        .set(ProductOrderDO::getTransactionNo, transactionId)
                        .set(ProductOrderDO::getOrderState,newState)
                        .set(OrderStateEnum.PAY.name().equals(newState),ProductOrderDO::getNotifyTime,new Date())
                        .eq(ProductOrderDO::getOutTradeNo, outTradeNo)
                        .notIn(ProductOrderDO::getOrderState,newState)); //排除目标状态，防止重复更新

        if(updated>0){
            log.info("更新订单成功, outTradeNo:{}, newState:{}", outTradeNo, newState);
            return updated;
        }else {
            log.warn("更新订单失败, outTradeNo:{}, newState:{}", outTradeNo, newState);
            return updated;
        }

    }


    private ProductOrderDO selectByOutTradeNo(String outTradeNo) {
        return productOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductOrderDO>()
                        .eq(ProductOrderDO::getOutTradeNo, outTradeNo)
        );
    }


    /** 构建微信回调失败响应 */
    private String buildFailResponse(String message) {
        return "{\"code\":\"FAIL\",\"message\":\"" + message + "\"}";
    }


    /**
     * 处理单权益订单发放 ：构建一个BenefitItem消息
     */
    private void processSingleBenefit(ProductOrderDO productOrderDO){
        //查找权益
        BenefitDO benefitDO = benefitMapper.selectById(productOrderDO.getBenefitId());
        if(benefitDO == null){
            log.warn("权益不存在, benefitId:{}", productOrderDO.getBenefitId());
            return;
        }

        //构建一个BenefitItem消息
        BenefitGrantMessage.BenefitItem benefitItemMessage = BenefitGrantMessage.BenefitItem.builder()
                .benefitId(benefitDO.getId())
                .benefitCode(benefitDO.getBenefitCode())
                .count(productOrderDO.getPurchaseCount())
                .validDays(benefitDO.getValidDays())
                .build();


        //构建一个BenefitGrantMessage
        BenefitGrantMessage benefitGrantMessage = BenefitGrantMessage.builder()
                .outTradeNo(productOrderDO.getOutTradeNo())
                .accountId(productOrderDO.getAccountId())
                .orderId(productOrderDO.getId())
                .orderType(productOrderDO.getOrderType())
                .benefitItems(List.of(benefitItemMessage))
                .build();

        //发送消息
        rabbitTemplate.convertAndSend(BenefitGrantMQConfig.BENEFIT_GRANT_EXCHANGE
        , BenefitGrantMQConfig.BENEFIT_GRANT_ROUTING_KEY, benefitGrantMessage);

        log.info("发送单权益订单发放消息成功, benefitGrantMessage:{}", benefitGrantMessage);
    }

    /**
     * 处理套餐订单发放，把套餐内的权益构建成List<BenefitItem>,只发送一条消息
     */
    private void processPackageBenefits(ProductOrderDO productOrderDO){

        List<PackageBenefitDO> packageBenefitDOList = packageBenefitMapper.selectList(new LambdaQueryWrapper<PackageBenefitDO>()
                .eq(PackageBenefitDO::getProductPackageId, productOrderDO.getProductPackageId()));

        if(packageBenefitDOList == null){
            log.warn("套餐内没有权益, packageId:{}", productOrderDO.getProductPackageId());
            return;
        }

        //批量查询套餐内的全部权益信息
        List<String> benefitCodeList = packageBenefitDOList.stream().map(PackageBenefitDO::getBenefitCode).toList();

        //用IN查询全部权益，将结果转化 为Map<BenefitCode, BenefitDO>
        Map<String, BenefitDO> benefitDOMap = benefitMapper.selectList(new LambdaQueryWrapper<BenefitDO>()
                        .in(BenefitDO::getBenefitCode, benefitCodeList))
                .stream().collect(Collectors.toMap(BenefitDO::getBenefitCode, b -> b));


        //构建BenefitItem列表
        List<BenefitGrantMessage.BenefitItem> benefitItems = packageBenefitDOList.stream()
                .map(packageBenefitDO -> {
                    BenefitDO benefitDO = benefitDOMap.get(packageBenefitDO.getBenefitCode());
                    return BenefitGrantMessage.BenefitItem.builder()
                            .benefitId(benefitDO.getId())
                            .benefitCode(benefitDO.getBenefitCode())
                            .count(packageBenefitDO.getCount())
                            .validDays(benefitDO.getValidDays())
                            .build();
                }).toList();

        //构建一个BenefitGrantMessage
        BenefitGrantMessage benefitGrantMessage = BenefitGrantMessage.builder()
                .outTradeNo(productOrderDO.getOutTradeNo())
                .accountId(productOrderDO.getAccountId())
                .orderId(productOrderDO.getId())
                .orderType(productOrderDO.getOrderType())
                .benefitItems(benefitItems)
                .build();

        //发送消息
        rabbitTemplate.convertAndSend(BenefitGrantMQConfig.BENEFIT_GRANT_EXCHANGE,
                BenefitGrantMQConfig.BENEFIT_GRANT_ROUTING_KEY, benefitGrantMessage);

        log.info("发送套餐订单发放消息成功, benefitGrantMessage:{}", benefitGrantMessage);

    }


    /**
     * 处理订单权益发放，支付成功后发送MQ消息到账号服务
     * @param outTradeNo
     */
    private void processOrderBenefits(String outTradeNo){

        ProductOrderDO productOrderDO = selectByOutTradeNo(outTradeNo);
        if(productOrderDO == null){
            log.warn("订单不存在, outTradeNo:{}", outTradeNo);
            return;
        }
        if(productOrderDO.getOrderType().equals(OrderTypeEnum.PACKAGE_ORDER.name())){
            processPackageBenefits(productOrderDO);
        }else if(productOrderDO.getOrderType().equals(OrderTypeEnum.BENEFIT_ORDER.name())){
            processSingleBenefit(productOrderDO);
        }else {
            log.warn("订单类型错误, outTradeNo:{}", outTradeNo);
        }


    }



}





