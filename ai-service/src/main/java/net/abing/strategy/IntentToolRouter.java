package net.abing.strategy;

import lombok.RequiredArgsConstructor;
import net.abing.component.tools.BenefitTools;
import net.abing.component.tools.InterviewTools;
import net.abing.component.tools.ResumeTools;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntentToolRouter {

    private final BenefitTools benefitTools;
    private final ResumeTools resumeTools;
    private final InterviewTools interviewTools;

    /**
     * 根据意图获取对应的Tool Bean
     * 通用对话返回null（不需要工具）
     */
    public Object getToolBean(String intentType) {
        return switch (intentType) {
            case "BENEFIT_QUERY" -> benefitTools;
            case "RESUME_ANALYSE" -> resumeTools;
            case "INTERVIEW" -> interviewTools;
            default -> null;
        };
    }


    /**
     * 根据意图获取系统提示词
     */
    public String getSystemPrompt(String intentType) {
        return switch (intentType) {
            case "BENEFIT_QUERY" -> """
                    你是一个权益服务助手，帮助用户查询权益余额、购买套餐和管理订单。
                    你可以使用工具来查询权益、商品列表，以及帮助用户下单购买。
                    请根据用户需求进行引导，在用户明确确认后再执行购买操作。
                    当用户权益不足时，主动推荐购买对应权益或套餐。
                    
                    【重要约束 - 支付二维码输出规则】
                    当购买工具返回包含 [PAY_QRCODE:...] 格式的内容时，你必须在回复中原样保留该标记。
                    格式示例：[PAY_QRCODE:{"codeUrl":"weixin://...","outTradeNo":"..."}]
                    前端会自动将该标记渲染为二维码图片，你不要尝试解释或改写该链接内容。
                    你只需在标记前后添加简短提示文字（如"请扫码支付"）即可。
                    
                    请使用中文回答，回复格式使用Markdown。
                    """;
            case "RESUME_ANALYSE" -> """
                    你是一个AI简历分析助手，帮助用户管理和分析简历。
                    你可以使用工具查询用户的简历列表，并对指定简历进行AI分析。
                    分析简历需要消耗1次简历分析权益，请在执行前告知用户。
                    只在用户明确确认后才执行分析操作。
                    请使用中文回答，回复格式使用Markdown。
                    """;
            case "INTERVIEW" -> """
                    你是一个AI面试助手，帮助用户创建AI模拟面试。
                    创建面试需要收集以下信息：简历ID、目标岗位、期望薪资、工作年限、目标城市、面试类型。
                    面试类型只能是 SPECIAL_INTERVIEW(专项面试) 或 COMMON_INTERVIEW(简历综合面)。
                    专项面试时还需要收集专项内容（如Redis、Spring Boot）。
                    请逐步引导用户提供信息，不要一次性问太多问题。
                    创建面试需要消耗面试权益，请提前告知用户。
                    只在用户确认所有信息无误后才执行创建。
                    请使用中文回答，回复格式使用Markdown。
                    """;
            default -> """
                    你是一个智能AI面试平台的助手，可以帮助用户：
                    1. 简历分析 - 上传和分析简历
                    2. AI模拟面试 - 创建和参加AI面试
                    3. 权益管理 - 查询和购买权益/套餐
                    
                    如果用户的问题涉及以上业务功能，请引导用户明确说出需求。
                    对于通用问题，请友好、专业地回答。
                    请使用中文回答，回复格式使用Markdown。
                    """;
        };
    }
}
