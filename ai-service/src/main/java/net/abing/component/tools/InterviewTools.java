package net.abing.component.tools;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.controller.req.InterviewCreateReq;
import net.abing.dto.AccountDTO;
import net.abing.dto.ResumeDTO;
import net.abing.interceptor.AccountLoginInterceptor;
import net.abing.service.InterviewService;
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
public class InterviewTools {



    private final InterviewService interviewService;
    private final ResumeService resumeService;

    @Tool(description = "查询用户的简历列表，创建面试前需要先选择一份简历")
    public String listResumesForInterview(ToolContext toolContext) {
        restoreContext(toolContext);
        List<ResumeDTO> resumeList = resumeService.list();
        if (resumeList == null || resumeList.isEmpty()) {
            return "用户当前没有上传任何简历，" +
                    "请提示用户先通过简历管理页面上传简历才能创建面试。";
        }
        StringBuilder sb = new StringBuilder("可选简历列表：\n");
        for (int i = 0; i < resumeList.size(); i++) {
            ResumeDTO resume = resumeList.get(i);
            sb.append(String.format("%d. 简历ID: %d, 文件名: %s\n",
                    i + 1, resume.getId(), resume.getFilename()));
        }
        return sb.toString();
    }



    @Tool(description = "创建AI模拟面试，需要收集完所有必填信息后才调用。会消耗面试权益。只在用户明确确认所有信息后才调用")
    public String createInterview(
            @ToolParam(description = "简历ID") Long resumeId,
            @ToolParam(description = "目标岗位，如Java开发工程师") String targetPosition,
            @ToolParam(description = "期望薪资，如15k-20k") String expectedSalary,
            @ToolParam(description = "工作年限，如3年") String workYears,
            @ToolParam(description = "目标城市，如北京") String targetCity,
            @ToolParam(description = "面试类型：SPECIAL_INTERVIEW(专项面试)或COMMON_INTERVIEW(简历综合面)") String interviewType,
            @ToolParam(description = "专项面内容，面试类型为专项时必填") String specialContent,
            ToolContext toolContext) {
        restoreContext(toolContext);
        try {
            InterviewCreateReq req = InterviewCreateReq.builder()
                    .resumeId(resumeId)
                    .targetPosition(targetPosition)
                    .expectedSalary(expectedSalary)
                    .workYears(workYears)
                    .targetCity(targetCity)
                    .interviewType(interviewType)
                    .specialContent(specialContent != null ? specialContent : "")
                    .build();

            JsonData result = interviewService.create(req);
            if (result.getCode() == 0) {
                return "面试创建成功！系统正在生成面试题目（约2-3分钟），" +
                        "请稍后前往个人中心→面试列表页面查看并开始面试。";
            }
            return "面试创建失败：" + result.getMsg() + "。请检查权益余额后重试。";
        } catch (Exception e) {
            log.error("创建面试异常, resumeId:{}", resumeId, e);
            return "面试创建时出现异常：" + e.getMessage();
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
