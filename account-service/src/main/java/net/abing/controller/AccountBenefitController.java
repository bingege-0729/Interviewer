package net.abing.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import net.abing.controller.req.AccountBenefitPageReq;
import net.abing.req.BenefitCheckReq;
import net.abing.req.BenefitTaskUpdateReq;
import net.abing.service.AccountBenefitService;
import net.abing.service.BenefitTaskService;
import net.abing.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户权益管理控制器
 */
@RestController
@RequestMapping("/api/v1/account/benefit")
@Slf4j
public class AccountBenefitController {

    @Autowired
    private AccountBenefitService accountBenefitService;

    @Autowired
    private BenefitTaskService benefitTaskService;


    /**
     * 检查并扣减用户权益
     *
     * 设计说明（方案1优化）：
     * - 只负责权益扣减和延迟检查
     * - 返回messageId供ai-service发送业务MQ
     *
     * @return 扣减结果
     */
    @Operation(summary = "检查并扣减用户权益", description = "扣减权益并返回messageId，用于后续发送业务MQ")
    @PostMapping("/checkAndDeduct")
    JsonData checkAndDeduct(@RequestBody BenefitCheckReq benefitCheckReq){
        return accountBenefitService.checkAndDeductBenefit(benefitCheckReq);
    }

    /**
     * 更新任务状态（由ai-service调用）
     * 支持完成和取消回滚两种操作
     *
     * @param req 状态更新请求
     * @return 更新结果
     */
    @Operation(summary = "更新任务状态", description = "更新权益任务状态，支持完成和取消回滚")
    @PostMapping("/task/updateStatus")
    JsonData updateTaskStatus(@RequestBody BenefitTaskUpdateReq req) {
        boolean success = benefitTaskService.updateTaskStatus(req);
        if (success) {
            return JsonData.buildSuccess();
        } else {
            return JsonData.buildError("任务状态更新失败");
        }
    }

    /**
     * 分页查询当前用户的权益列表
     */
    @Operation(summary = "分页查询权益列表", description = "分页查询当前登录用户的权益记录")
    @PostMapping("/page")
    public JsonData page(@RequestBody AccountBenefitPageReq req) {
        return accountBenefitService.page(req);
    }



    /**
     * 查询指定用户的有效权益列表（内部服务调用）拦截器放行 "/api/v1/account/benefit/list",
     * 返回未过期且剩余次数>0的权益汇总
     */
    @Operation(summary = "查询用户有效权益", description = "根据用户ID查询有效权益列表，供内部服务调用")
    @GetMapping("/list")
    public JsonData listByAccountId(@RequestParam("accountId") Long accountId) {
        return accountBenefitService.listByAccountId(accountId);
    }
}