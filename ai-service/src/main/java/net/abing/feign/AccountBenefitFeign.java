package net.abing.feign;

import io.swagger.v3.oas.annotations.Operation;
import net.abing.req.BenefitCheckReq;
import net.abing.req.BenefitTaskUpdateReq;
import net.abing.util.JsonData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 账户权益服务Feign客户端
 *
 * 设计说明：
 * ai-service调用account-service扣减权益
 * account-service只负责权益扣减和延迟检查
 * ai-service负责发送业务MQ消息
 */
@FeignClient(name = "account-service", contextId = "AccountBenefitService")
public interface AccountBenefitFeign {

    /**
     * 检查并扣减用户权益
     *
     * @param benefitCheckReq 权益检查请求
     * @return 扣减结果（包含messageId，用于发送业务MQ）
     */
    @Operation(summary = "检查并扣减用户权益", description = "扣减权益并返回messageId，用于后续发送业务MQ")
    @PostMapping("/api/v1/account/benefit/checkAndDeduct")
    JsonData checkAndDeductBenefit(@RequestBody BenefitCheckReq benefitCheckReq);

    /**
     * 更新任务状态
     * 支持完成和取消回滚两种操作
     *
     * @param req 状态更新请求
     * @return 更新结果
     */
    @Operation(summary = "更新任务状态", description = "更新权益任务状态，支持完成和取消回滚")
    @PostMapping("/api/v1/account/benefit/task/updateStatus")
    JsonData updateTaskStatus(@RequestBody BenefitTaskUpdateReq req);



    /**
     * 分页查询用户权益列表
     */
    @PostMapping("/api/v1/account/benefit/page")
    JsonData pageBenefits(@RequestBody Map<String, Object> req);

    /**
     * 查询指定用户的有效权益列表（内部服务调用，不需要登录token）
     * @param accountId 用户ID
     * @return 有效权益汇总列表
     */
    @GetMapping("/api/v1/account/benefit/list")
    JsonData listByAccountId(@RequestParam("accountId") Long accountId);

}