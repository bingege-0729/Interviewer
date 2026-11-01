package net.abing.component.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.abing.controller.req.InterviewCreateReq;
import net.abing.dto.AIInterviewRoundDTO;
import net.abing.dto.AIResumeInfoDTO;
import net.abing.dto.QuestionDTO;
import net.abing.dto.UserProfileDTO;
import net.abing.enums.BenefitEnum;
import net.abing.model.InterviewRoundDO;
import net.abing.util.JsonUtil;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 面试生成编排器
 */


@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewGenerationOrchestrator {


    private final ChatModel chatModel;


    public PipelineResult generate(String resumeContent, InterviewCreateReq req) {

        try {

            UserProfileDTO userProfileDTO = UserProfileDTO.builder()
                    .expectedSalary(req.getExpectedSalary())
                    .workYears(req.getWorkYears())
                    .targetCity(req.getTargetCity())
                    .targetPosition(req.getTargetPosition())
                    .interviewType(req.getInterviewType())
                    .specialContent(req.getSpecialContent())
                    .resumeId(req.getResumeId())
                    .build();


            //创建简历解析 agent
            ReactAgent resumeAgent = ReactAgent.builder()
                    .name("ResumeParserAgent")
                    .model(chatModel)
                    .instruction(buildResumeInstruction())
                    .outputType(AIResumeInfoDTO.class)
                    .outputKey("resume_info")
                    .includeContents(false)
                    .build();

            //面试轮次创建agent
            ReactAgent roundAgent = ReactAgent.builder()
                    .name("round_generator")
                    .model(chatModel)
                    .instruction(buildRoundInstruction(userProfileDTO))
                    .outputType(RoundsResult.class)
                    .outputKey("round_info")
                    .includeContents(false)
                    .build();

            //编排agent
            SequentialAgent pipeline = SequentialAgent.builder()
                    .name("InterviewGenerationPipeline")
                    .description("面试生成流水线:简历解析->轮次生成")
                    .subAgents(List.of(resumeAgent, roundAgent))
                    .build();
            log.info("开始执行面试生成流水线");
            Optional<OverAllState> overAllStateOpt = pipeline.invoke(resumeContent);
            if(overAllStateOpt.isEmpty()){
                log.error("面试生成流水线执行失败");
                return null;
            }

            //从OverAllState 获取结构化的数据
            PipelineResult pipelineResult = extractResult(overAllStateOpt.get(),resumeContent);
            log.info("面试生成流水线执行成功，结果：{}", pipelineResult);
            return pipelineResult;

        }catch (Exception e){
            log.error("生成面试失败: {}", e.getMessage());
            return null;
        }

    }


    /**
     * 每个轮次生成的面试题
     */
    public Map<Long,List<QuestionDTO>> generateQuestions(InterviewCreateReq req, AIResumeInfoDTO resumeInfoDTO,
                                                         List<InterviewRoundDO> rounds) {


        //每个轮次构建面试题生成的agent
        List<Agent> questionAgents = new ArrayList<>();
        for(InterviewRoundDO round : rounds){

            String prompt = buildQuestionPrompt(req, resumeInfoDTO, round);

            ReactAgent questionAgent = ReactAgent.builder()
                    .name("question_round_"+round.getRoundNumber())
                    .model(chatModel)
                    .instruction(prompt)
                    .outputType(QuestionResult.class)
                    .outputKey("round_"+round.getId()+"_questions")
                    .includeContents(false)
                    .build();
            questionAgents.add(questionAgent);
        }


        //创建并行agent
        ParallelAgent parallelAgent = ParallelAgent.builder()
                .name("question_parallel_generator")
                .description("并行生成各个轮次面试题")
                .subAgents(questionAgents)
                .build();

        log.info("开始并行生成各个轮次面试题，轮次数量:{}",rounds.size());

        try {
            Optional<OverAllState> overAllState = parallelAgent.invoke("生成面试题");

            return extractQuestion(overAllState.get(),rounds);
        } catch (GraphRunnerException e) {
            log.error("并行生成各个轮次面试题异常，{}",e);
            return null;
        }
    }

    private Map<Long, List<QuestionDTO>> extractQuestion(OverAllState overAllState, List<InterviewRoundDO> rounds) {
        Map<Long,List<QuestionDTO>> result = new HashMap<>();
        for(InterviewRoundDO roundDO : rounds){
            String key = "round_"+roundDO.getId()+"_questions";
            try {
                overAllState.value(key).ifPresent(value->{
                    String jsonStr = extractTextContent(value);
                    QuestionResult questionResult = JsonUtil.json2Obj(jsonStr, QuestionResult.class);
                    result.put(roundDO.getId(),questionResult.getQuestions());
                });
            }catch (Exception e){
                log.error("提取{}轮次 题目失败:{}",roundDO.getId(),e);
            }
        }

        return result;
    }

    /**
     * 构建题目生成提示词，难度根据期望薪资和工作年限自动匹配
     */
    private String buildQuestionPrompt(InterviewCreateReq req, AIResumeInfoDTO resumeInfoDTO, InterviewRoundDO round) {
        return String.format("""
                    你是阿里、腾讯、字节等大厂的高级技术专家和资深面试官，需要基于目标岗位要求生成精准的面试题目。
                    
                    **应聘者基本信息：**
                    - 目标岗位：%s
                    - 工作年限：%s
                    - 期望薪资：%s
                    - 目标城市：%s
                    - 简历内容：%s
                    
                    **当前面试轮次信息：**
                    - 轮次序号：第%d轮
                    - 轮次类型：%s
                    - 轮次描述：%s
                    - 要求题目数量：%d道
                    
                    **题目生成核心原则：**
                    **1. 目标岗位优先原则**
                    - 题目生成必须以目标岗位技能要求为核心出发点
                    - 重点考察目标岗位所需的核心技能和能力
                    - 结合期望薪资和工作年限，确定题目难度等级
                    
                    **2. 简历匹配度智能分析**
                    - 分析目标岗位的核心技能要求
                    - 评估候选人的技能与目标岗位的匹配度
                    - 设计针对性的题目来验证目标岗位能力
                    
                    **3. 项目面连环提问策略**
                    - 基于简历中的具体项目和个人职责进行深度挖掘
                    - 设计连环提问，每个题目深入2-3次追问
                    - 从项目背景 → 技术选型 → 架构设计 → 性能优化 → 问题解决 → 经验总结
                    
                    **难度与薪资匹配标准：**
                    - 初级（0-3年，薪资10-20K）：基础概念、简单算法、基础项目
                    - 中级（3-5年，薪资20-35K）：中等算法、复杂项目、系统设计基础
                    - 高级（5-8年，薪资35-50K）：高级算法、架构设计、技术选型
                    - 专家（8年以上，薪资50K+）：复杂系统设计、技术领导力、创新思维
                    
                    **题目生成具体要求：**
                    1. 严格按照"%s"轮次特点生成题目，必须围绕目标岗位技能要求，采用MarkDown格式输出题目
                    2. 基于目标岗位%s，结合期望薪资%s和工作年限%s，匹配相应难度级别
                    3. 题目必须贴近实际工作场景，避免纯理论
                    4. 每道题目都要有明确的考察点和评分标准
                    5. 生成%d道题目，符合轮次要求
                    
                    **题目规格：**
                    - 每道题目100分满分
                    - 每道题目的回答时间根据题目难度从3分钟到30分钟之间
                    - 提供详细的题型说明、考察重点和标准答案
                    
                    **评分标准：**
                    - 技术准确性（30%%）
                    - 实际应用能力（25%%）
                    - 问题解决思路（25%%）
                    - 代码质量（20%%）
                    
                    请基于目标岗位要求，结合期望薪资和工作年限，生成符合上述要求的专业面试题目列表。
                    严格生成%d道题目。
                    
                    **输出格式要求：**
                    请严格按照JSON对象格式输出，包含 "questions" 字段，值为题目数组。
                    每个题目包含字段：content(题目内容), type(题型 SHORT_ANSWER / CODING), difficulty(难度 EASY / MEDIUM / HARD), category(分类), standardAnswer(标准答案), keyPoints(关键考察点), maxScore(满分,默认100), timeLimit(作答时间,分钟)
                    示例格式：("questions": [("content": "...", "type": "...", "difficulty": "...", "category": "...", "standardAnswer": "...", "keyPoints": "...", "maxScore": 100, "timeLimit": 10)])
                    """,
                req.getTargetPosition() != null ? req.getTargetPosition() : "未指定",
                req.getWorkYears() != null ? req.getWorkYears() : "未指定",
                req.getExpectedSalary() != null ? req.getExpectedSalary() : "未指定",
                req.getTargetCity() != null ? req.getTargetCity() : "未指定",
                sanitizeForTemplate(resumeInfoDTO.getRawContent() != null ? resumeInfoDTO.getRawContent() : "未指定"),
                round.getRoundNumber() != null ? round.getRoundNumber() : 1,
                round.getRoundType() != null ? round.getRoundType() : "综合面试",
                round.getDescription() != null ? round.getDescription() : "综合能力考察",
                round.getTotalQuestion() != null ? round.getTotalQuestion() : 5,
                round.getRoundType() != null ? round.getRoundType() : "综合面试",
                req.getTargetPosition() != null ? req.getTargetPosition() : "未指定",
                req.getExpectedSalary() != null ? req.getExpectedSalary() : "未指定",
                req.getWorkYears() != null ? req.getWorkYears() : "未指定",
                round.getTotalQuestion() != null ? round.getTotalQuestion() : 5,
                round.getTotalQuestion() != null ? round.getTotalQuestion() : 5
        );
    }

    /**
     * 清理文本中的ST4模板特殊字符，防止被StringTemplate引擎误解析
     * <p>将花括号替换为圆括号，避免与ST4的 {placeholder} 语法冲突</p>
     */
    private String sanitizeForTemplate(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("{", "(").replace("}", ")");
    }

    /**
     * 从对应的OverAllState中提取结构化数据
     * @param state
     * @param resumeContent
     * @return
     */
    private PipelineResult extractResult(OverAllState state, String resumeContent) {
        PipelineResult pipelineResult = new PipelineResult();

        //  提取简历信息
        try {
            state.value("resume_info").ifPresent(value->{
                String jsonStr = extractTextContent(value);
                AIResumeInfoDTO aiResumeInfoDTO = JsonUtil.json2Obj(jsonStr, AIResumeInfoDTO.class);
                if(aiResumeInfoDTO != null){
                    aiResumeInfoDTO.setRawContent(resumeContent);
                    pipelineResult.setResumeInfo(aiResumeInfoDTO);
                }
            });

        }catch (Exception e){
            log.error("提取简历信息失败: {}", e.getMessage());
        }
        //提取面试轮次
        try {
            state.value("round_info").ifPresent(value->{
                String jsonStr = extractTextContent(value);
                RoundsResult roundsResult = JsonUtil.json2Obj(jsonStr, RoundsResult.class);
                if(roundsResult != null){
                    pipelineResult.setRoundDTOS(roundsResult.getRoundDTOS());
                }
            });
        }catch (Exception e){
            log.error("提取面试轮次失败: {}", e.getMessage());
            List<AIInterviewRoundDTO> interviewRoundDTOS = generateDefaultRounds();
            pipelineResult.setRoundDTOS(interviewRoundDTOS);
        }

      return pipelineResult;
    }


    private String extractTextContent(Object value) {
        if(value instanceof  AssistantMessage message){
            return message.getText();
        }
        return value.toString();
    }



    private String buildRoundInstruction(UserProfileDTO userProfile) {

        boolean isSpecial = BenefitEnum.SPECIAL_INTERVIEW.name().equals(userProfile.getInterviewType());

        if (isSpecial) {
            return String.format("""
                    你是一位具备15年以上大厂技术面试经验的资深面试官，曾任职于阿里、腾讯、字节等顶级互联网公司。
                    现在需要为以下候选人设计一套针对性的专项面试轮次方案。
                    
                    **候选人简历信息（由上一步简历解析 Agent 输出）：**
                    {resume_info}
                    
                    **专项面试目标内容：**
                    %s
                    
                    **专项面试设计原则：**
                    1. 面试轮次设计必须紧紧围绕用户指定的专项内容
                    2. 深度挖掘专项技能的技术细节和实践应用
                    3. 从基础到高级，层层递进考察专项能力
                    4. 结合实际工作场景设计专项题目
                    
                    **面试轮次设计要求：**
                    请设计3-4轮专项面试轮次，每轮包含：轮次名称、轮次目标、考察维度、题目数量（2-4题）。
                    请生成四轮面试。
                    请严格按照JSON对象格式输出，包含 "rounds" 字段，值为数组，每个元素包含字段：roundNumber, roundType, description, totalQuestion
                    示例格式：("rounds": [("roundNumber": 1, "roundType": "...", "description": "...", "totalQuestion": 3)])
                    """,
                    userProfile.getSpecialContent() != null ? userProfile.getSpecialContent() : "");
        } else {
            return String.format("""
                    你是一位具备15年以上大厂技术面试经验的资深面试官，曾任职于阿里、腾讯、字节等顶级互联网公司。
                    现在需要为以下候选人设计一套科学、专业、针对性强的面试轮次方案。
                    
                    **候选人简历信息（由上一步简历解析 Agent 输出）：**
                    {resume_info}
                    
                    **候选人求职目标岗位信息：**
                    - 期望薪资：%s
                    - 工作年限：%s
                    - 目标城市：%s
                    - 目标岗位：%s
                    
                    **面试轮次设计核心原则：**
                    1. 目标岗位优先原则：面试轮次设计必须以目标岗位为核心出发点
                    2. 岗位匹配度分析：评估候选人技能与目标岗位的匹配度
                    3. 差异化设计策略：匹配度高重点考察深度和高级能力，匹配度低重点考察基础能力和学习潜力
                    
                    **面试轮次设计要求：**
                    请设计3-4轮面试轮次，每轮包含：轮次名称（体现岗位技术栈）、轮次目标、考察维度、题目数量（3-4题）。
                    请生成四轮面试。
                    请严格按照JSON对象格式输出，包含 "rounds" 字段，值为数组，每个元素包含字段：roundNumber, roundType, description, totalQuestion
                    示例格式：("rounds": [("roundNumber": 1, "roundType": "...", "description": "...", "totalQuestion": 3)])
                    """,
                    userProfile.getExpectedSalary(),
                    userProfile.getWorkYears(),
                    userProfile.getTargetCity(),
                    userProfile.getTargetPosition());
        }
    }


    private String buildResumeInstruction() {
        return """
            你是一位资深的简历分析专家，请解析以下简历内容，提取关键信息。
            
            **简历内容：**
            {input}
            
            **请提取以下信息：**
            1. **userName**：候选人姓名
            2. **education**：教育经历（学校、专业、学历）
            3. **workYears**：工作年限
            4. **skillTags**：技术技能标签列表
            5. **projectExperiences**：项目经验列表
            6. **rawContent**：简历原始全部内容
            
            **输出要求：**
            - 技能标签需要提取所有出现的技术关键词
            - 项目经验需要提取项目名称和关键技术点
            - 如果某项信息无法从简历中提取，请使用合理的默认值
            - rawContent 字段直接返回简历原始内容
            """;
    }


    /**
     * 轮次生成的outputType，包装类，因为包装类不支持直接使用List 范型
     */
    @Data
    public static class RoundsResult{
        private List<AIInterviewRoundDTO> roundDTOS;
    }


    /**
     * 面试题生成的outputType，包装类，因为包装类不支持直接使用List 范型
     */
    @Data
    public static class QuestionResult{
        private List<QuestionDTO> questions;
    }



    @Data
    public static class PipelineResult {
        /**
         * 简历AI解析后的结果
         */
        private AIResumeInfoDTO resumeInfo;

        /**
         * 生成多个轮次
         */
        private List<AIInterviewRoundDTO> roundDTOS;


    }

    private List<AIInterviewRoundDTO> generateDefaultRounds() {
        return List.of(
                AIInterviewRoundDTO.builder()
                        .roundNumber(1).roundType("技术基础面")
                        .description("基础技术能力考察").totalQuestion(3).build(),
                AIInterviewRoundDTO.builder()
                        .roundNumber(2).roundType("项目经验面")
                        .description("实际项目能力和团队协作能力考察").totalQuestion(2).build(),
                AIInterviewRoundDTO.builder()
                        .roundNumber(3).roundType("综合评估面")
                        .description("最终评估候选人是否适合岗位").totalQuestion(2).build()
        );
    }
}
