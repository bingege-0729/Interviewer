package net.abing.component.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.dto.AccountDTO;
import net.abing.dto.ResumeDTO;
import net.abing.enums.BenefitEnum;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.req.BenefitCheckReq;
import net.abing.service.ResumeService;
import net.abing.util.JsonData;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class ResumeTools {

    private final ResumeService resumeService;

    @Tool(description = "查询用户已上传的简历列表，返回简历ID和文件名")
    public String listResumes(ToolContext toolContext) {
        restoreContext(toolContext);
        List<ResumeDTO> resumeList = resumeService.list();
        if (resumeList == null || resumeList.isEmpty()) {
            return "用户当前没有上传任何简历，请提示用户先通过简历管理页面上传简历。";
        }
        StringBuilder sb = new StringBuilder("用户的简历列表：\n");
        for (int i = 0; i < resumeList.size(); i++) {
            ResumeDTO resume = resumeList.get(i);
            sb.append(String.format("%d. 简历ID: %d, 文件名: %s\n",
                    i + 1, resume.getId(), resume.getFilename()));
        }
        return sb.toString();
    }



    @Tool(description = "对指定简历进行AI分析，会消耗1次简历分析权益。只在用户明确确认后才调用")
    public String analyseResume(@ToolParam(description = "要分析的简历ID") Long resumeId,
                                ToolContext toolContext) {
        restoreContext(toolContext);
        Long accountId = (Long) toolContext.getContext().get("accountId");
        try {
            BenefitCheckReq checkReq = BenefitCheckReq.builder()
                    .benefitCode(BenefitEnum.RESUME_ANALYSE.name())
                    .businessId(resumeId.toString())
                    .accountId(accountId)
                    .count(1)
                    .build();

            JsonData result = resumeService.analyse(checkReq);
            if (result.getCode() == 0) {
                return "简历分析已启动，系统正在分析中（约1-2分钟），" +
                        "请稍后前往个人中心→简历管理页面查看分析报告。";
            }
            return "简历分析失败：" + result.getMsg() + "。请检查简历分析权益余额。";
        } catch (Exception e) {
            log.error("简历分析异常, resumeId:{}", resumeId, e);
            return "简历分析时出现异常：" + e.getMessage();
        }
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
