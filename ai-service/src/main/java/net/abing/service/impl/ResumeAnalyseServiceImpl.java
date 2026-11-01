package net.abing.service.impl;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.dto.ResumeAnalyseMessageDTO;
import net.abing.dto.ResumeAnalysisResultDTO;
import net.abing.enums.BenefitEnum;
import net.abing.enums.ResumeStatus;
import net.abing.mapper.ResumeMapper;
import net.abing.model.ResumeDO;
import net.abing.service.BenefitTaskService;
import net.abing.service.ResumeAnalyseService;
import net.abing.util.JsonUtil;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalyseServiceImpl implements ResumeAnalyseService {


    private final ResumeMapper resumeMapper;
    private final ChatModel chatModel;
    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;
    private final BenefitTaskService benefitTaskService;


    private ReactAgent reactAgent;
    private BeanOutputConverter<ResumeAnalysisResultDTO> outputConverter;


    /**
     * 初始化MCP服务，加载工具回调，设置输出转换器
     */
    @PostConstruct
    public void init(){
        if (toolCallbackProvider != null) {
            ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
            log.info("reactAgent初始化，加载了 MCP工具: {} 个", toolCallbacks.length);
        } else {
            log.warn("ToolCallbackProvider 未注入（MCP 客户端已禁用），简历分析的 MCP 工具调用将不可用");
        }
        outputConverter = new BeanOutputConverter<>(ResumeAnalysisResultDTO.class);
        reactAgent = ReactAgent.builder()
                .name("mcp-react-agent")
                .model(chatModel)
                .outputSchema(outputConverter.getJsonSchema())
                .systemPrompt("你是一个专业的简历分析助手，使用MCP服务进行分析用户的简历，请严格按照JSON Schema格式返回分析结果")
                .build();
    }


    @Override
    public boolean processResumeAnalyse(ResumeAnalyseMessageDTO messageDTO) {

        long resumeId = messageDTO.getResumeId();
        String messageId = messageDTO.getMessageId();
        String businessId = String.valueOf(resumeId);

        //查找简历
        ResumeDO resumeDO = resumeMapper.selectOne(new LambdaQueryWrapper<ResumeDO>().eq(ResumeDO::getId, resumeId).eq(ResumeDO::getAccountId, messageDTO.getAccountId()));

        if (resumeDO == null) {
            log.error("Resume not found by resumeId: {}", resumeId);
            benefitTaskService.rollback(messageId, businessId, BenefitEnum.RESUME_ANALYSE.name(), "Resume not found");
            return false;
        }
        //调用MCP服务
        ResumeAnalysisResultDTO resumeAnalysisResultDTO = analyseResume(resumeDO);
        if(resumeAnalysisResultDTO == null){
            resumeDO.setStatus(ResumeStatus.ERROR.name());
            resumeMapper.updateById(resumeDO);
            benefitTaskService.rollback(messageId, businessId, BenefitEnum.RESUME_ANALYSE.name(), "MCP service error");
            return false;
        }
        //更新结果
        resumeDO.setStatus(ResumeStatus.ANALYZED.name());
        resumeDO.setEvaluation(JsonUtil.obj2Json(resumeAnalysisResultDTO));
        resumeDO.setSkillTags(JsonUtil.obj2Json(resumeAnalysisResultDTO.getSkillTags()));
        resumeMapper.updateById(resumeDO);

        //标记任务完成
        boolean markSuccess = benefitTaskService.markFinish(messageId,businessId, BenefitEnum.RESUME_ANALYSE.name());
        if (!markSuccess) {
            log.error("标记任务完成失败，等待延迟对列补偿检查: messageId{},resumeId:{}", messageId, resumeId);
            return true;
        }
        log.info("简历分析完成，resumeId:{}", resumeId);
        return true;
    }



    private  ResumeAnalysisResultDTO analyseResume(ResumeDO resumeDO) {
        try {
            String text = reactAgent.call(resumeDO.getContent()).getText();
            log.info("MCP服务返回结果: {}", text);
            return outputConverter.convert(text);
        }catch (Exception e){
            log.error("MCP服务调用异常: {}", e.getMessage());
            return null;
        }
    }
}
