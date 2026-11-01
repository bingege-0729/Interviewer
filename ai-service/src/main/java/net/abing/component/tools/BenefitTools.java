package net.abing.component.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.dto.AccountDTO;
import net.abing.enums.PaymentEnum;
import net.abing.feign.AccountBenefitFeign;
import net.abing.feign.ProductFeign;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.util.JsonData;
import net.abing.util.JsonUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class BenefitTools {

    private final ProductFeign productFeign;
    private final AccountBenefitFeign accountBenefitFeign;


    @Tool(description = "查询用户当前拥有的权益余额列表，包括简历分析次数、面试次数等")
    public String queryUserBenefits(ToolContext toolContext) {
        restoreContext(toolContext);
        Long accountId = (Long) toolContext.getContext().get("accountId");
        log.info("Tool调用: 查询用户权益, accountId:{}", accountId);
        JsonData result = accountBenefitFeign.listByAccountId(accountId);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return JsonUtil.obj2Json(result.getData());
        }
        return "暂无权益数据";
    }

    @Tool(description = "查询平台可购买的权益商品列表，返回权益ID、名称、价格等信息")
    public String listBenefits(ToolContext toolContext) {
        restoreContext(toolContext);
        JsonData result = productFeign.listBenefits();
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return JsonUtil.obj2Json(result.getData());
        }
        return "暂无可购买的权益";
    }

    @Tool(description = "查询平台可购买的套餐列表，返回套餐ID、名称、包含权益、价格等信息")
    public String listPackages(ToolContext toolContext) {
        restoreContext(toolContext);
        JsonData result = productFeign.listPackages();
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return JsonUtil.obj2Json(result.getData());
        }
        return "暂无可购买的套餐";
    }



    @Tool(description = "购买权益商品，创建微信支付订单并返回支付二维码信息。只在用户明确确认后才调用")
    public String buyBenefit(@ToolParam(description = "权益商品ID") Long benefitId,
                             @ToolParam(description = "购买数量，默认1") int purchaseCount,
                             ToolContext toolContext) {
        restoreContext(toolContext);
        log.info("Tool调用: 购买权益, benefitId:{}, count:{}", benefitId, purchaseCount);
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("benefitId", benefitId);
            req.put("purchaseCount", purchaseCount);
            req.put("payType", PaymentEnum.WECHAT_PAY.name());

            JsonData result = productFeign.createBenefitOrder(req);
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                // 关键：返回特殊标记格式，前端会解析渲染为二维码
                return "[PAY_QRCODE:" + JsonUtil.obj2Json(result.getData()) + "]";
            }
            return "订单创建失败：" + (result != null ? result.getMsg() : "服务异常");
        } catch (Exception e) {
            log.error("购买权益异常, benefitId:{}", benefitId, e);
            return "订单创建异常：" + e.getMessage();
        }
    }


    @Tool(description = "购买套餐，创建微信支付订单并返回支付二维码信息。只在用户明确确认后才调用")
    public String buyPackage(@ToolParam(description = "套餐ID") Long packageId,
                             ToolContext toolContext) {
        restoreContext(toolContext);
        Map<String, Object> req = new HashMap<>();
        req.put("packageId", packageId);
        req.put("payType", PaymentEnum.WECHAT_PAY.name());

        JsonData result = productFeign.createPackageOrder(req);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return "[PAY_QRCODE:" + JsonUtil.obj2Json(result.getData()) + "]";
        }
        return "订单创建失败：" + (result != null ? result.getMsg() : "服务异常");
    }


    @Tool(description = "查询订单支付状态，返回订单号、商品名称、金额、支付状态等信息")
    public String queryOrderStatus(@ToolParam(description = "订单号") String outTradeNo,
                                   ToolContext toolContext) {
        restoreContext(toolContext);
        JsonData result = productFeign.getOrderStatus(outTradeNo);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return JsonUtil.obj2Json(result.getData());
        }
        return "订单不存在或已过期";
    }




    private void restoreContext(ToolContext toolContext) {
        // 恢复HTTP请求属性（Feign拦截器需要）
        Object attrs = toolContext.getContext().get("requestAttributes");
        if (attrs instanceof RequestAttributes ra) {
            RequestContextHolder.setRequestAttributes(ra);
        }
        // 恢复登录用户信息（Service层需要）
        Object account = toolContext.getContext().get("currentAccount");
        if (account instanceof AccountDTO acc) {
            AccountLoginInterceptor.threadLocal.set(acc);
        }
    }
}
