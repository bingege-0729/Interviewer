package net.xdclass.component.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.statistics.QuestionStatistics;
import net.xdclass.component.statistics.QuestionStatisticsCalculator;
import net.xdclass.dto.RoundEvaluationDTO;
import net.xdclass.enums.RoundStatusEnum;
import net.xdclass.mapper.InterviewRoundMapper;
import net.xdclass.mapper.QuestionMapper;
import net.xdclass.model.InterviewRoundDO;
import net.xdclass.model.QuestionDO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RoundEvaluationAgent {


    private final ChatClient chatClient;

    private final QuestionStatisticsCalculator statisticsCalculator;

    @Autowired
    private  InterviewRoundMapper interviewRoundMapper;

    @Autowired
    private QuestionMapper questionMapper;

    public  RoundEvaluationAgent(ChatClient.Builder builder, QuestionStatisticsCalculator statisticsCalculator){
        this.chatClient = builder.build();
        this.statisticsCalculator = statisticsCalculator;
    }


    /**
     * 检查并生成轮次评价
     * <p>判断该轮次所有题目是否已评估完成，若是则异步生成轮次评价</p>
     *
     * @param interviewRoundId     轮次ID（由调用方直接传递，避免冗余查询）
     * @param interviewId 面试ID
     * @param accountId   用户ID
     */
    public void checkAndGenerate(Long interviewRoundId, Long interviewId, Long accountId) {
        try {
            InterviewRoundDO interviewRoundDO = interviewRoundMapper.selectById(interviewRoundId);
            if(interviewRoundDO != null && interviewRoundDO.getStatus().equals(RoundStatusEnum.COMPLETED.getCode())){
                log.info("轮次已评估完成，轮次ID：{}，面试ID：{}，用户ID：{}", interviewRoundId, interviewId, accountId);
                return;
            }

            //查询轮次下的全部题目
            List<QuestionDO> questionDOList = questionMapper.selectList(new LambdaQueryWrapper<QuestionDO>()
                    .eq(QuestionDO::getInterviewRoundId, interviewRoundId));


            //统计已经评估完成的题目
            List<QuestionDO> evaluatedQuestions = questionDOList.stream().filter(
                    questionDO -> questionDO.getStatus().equals(RoundStatusEnum.COMPLETED.getCode())).toList();


            //判断全部题目是否都完成了
            if(evaluatedQuestions.size() == questionDOList.size()){
                log.info("轮次所有题目已评估完成，轮次ID：{}，面试ID：{}，用户ID：{}", interviewRoundId, interviewId, accountId);
                generateAsync(interviewRoundDO,interviewId,accountId,evaluatedQuestions);
            }else {
                log.info("轮次部分题目已评估完成，轮次ID：{}，面试ID：{}，用户ID：{}", interviewRoundId, interviewId, accountId);
            }

        }catch (Exception e){
            log.error("检查并生成轮次评价异常，轮次ID：{}，面试ID：{}，用户ID：{}", interviewRoundId, interviewId, accountId, e);
        }

    }

    /**
     * 异步生成轮次评价
     * <p>流程：更新状态为评估中 → 统计题目数据 → AI生成评价 → 保存评价结果</p>
     *
     * @param interviewRoundDO           轮次对象（由 checkAndGenerate 直接传递，避免重复查询）
     * @param interviewId       面试ID
     * @param accountId         用户ID
     * @param evaluatedQuestions 已评估完成的题目列表
     */
    @Async("aiInterviewExecutor")
    private void generateAsync(InterviewRoundDO interviewRoundDO, Long interviewId, Long accountId, List<QuestionDO> evaluatedQuestions) {

        Long roundId = interviewRoundDO.getId();
        try {

            log.info("开始生成轮次评价，轮次ID：{}，面试ID：{}，用户ID：{}", roundId, interviewId, accountId);
            updateRoundStatus(roundId, RoundStatusEnum.EVALUATING);

            //构建题目统计信息和统计数据
            QuestionStatistics stats = statisticsCalculator.buildStatistics(evaluatedQuestions);

            //构建提示词
            String prompt = buildPrompt(interviewRoundDO, evaluatedQuestions.size(), stats);


            //调用AI生成
            RoundEvaluationDTO roundEvaluationDTO = chatClient.prompt().user(prompt).call().entity(RoundEvaluationDTO.class);

            if(roundEvaluationDTO != null){
                InterviewRoundDO roundDO = InterviewRoundDO.builder()
                        .id(roundId)
                        .overallScore(BigDecimal.valueOf(stats.getAverageScore()))
                        .answeredQuestions(evaluatedQuestions.size())
                        .status(RoundStatusEnum.COMPLETED.getCode())
                        .summary(roundEvaluationDTO.getEvaluation())
                        .strength(roundEvaluationDTO.getStrengths())
                        .improvement(roundEvaluationDTO.getImprovements())
                        .suggestion(roundEvaluationDTO.getSuggestions())
                        .build();
                interviewRoundMapper.updateById(roundDO);
                log.info("轮次评价生成完成，轮次ID：{}，面试ID：{}，用户ID：{}", roundId, interviewId, accountId);
            }else {
                log.error("轮次评价生成失败，轮次ID：{}，面试ID：{}，用户ID：{}", roundId, interviewId, accountId);
                updateRoundStatus(roundId, RoundStatusEnum.FAILED);
            }

        }catch (Exception e){
            log.error("异步生成轮次评价异常，轮次ID：{}，面试ID：{}，用户ID：{}", roundId, interviewId, accountId, e);
            updateRoundStatus(roundId, RoundStatusEnum.FAILED);
        }

    }

    private void updateRoundStatus(Long roundId,RoundStatusEnum status){
        interviewRoundMapper.updateById(InterviewRoundDO.builder().id(roundId).status(status.getCode()).build());
    }



    /**
     * 构建轮次评价提示词，从技术能力、问题解决、沟通表达、学习潜力4个维度评价
     */
    private String buildPrompt(InterviewRoundDO roundDO, int answeredCount, QuestionStatistics stats) {
        return String.format("""
                    作为一名资深技术面试官，请基于以下轮次面试情况，生成全面的轮次评价报告。
                    
                    **轮次基本信息：**
                    - 轮次序号：第%d轮
                    - 轮次类型：%s
                    - 轮次描述：%s
                    - 总题目数：%d
                    - 已回答题目数：%d
                    - 平均分数：%.1f分
                    
                    **详细题目回答情况：**
                    %s
                    
                    **轮次评价要求：**
                    请从以下维度进行综合评价：
                    
                    **1. 技术能力评估**
                    - 技术知识掌握程度
                    - 实际应用能力
                    - 技术深度和广度
                    
                    **2. 问题解决能力**
                    - 分析问题的逻辑性
                    - 解决方案的合理性
                    
                    **3. 沟通表达能力**
                    - 表达清晰度
                    - 逻辑结构
                    
                    **4. 学习成长潜力**
                    - 学习能力
                    - 持续改进意识
                    
                    **输出格式要求：**
                    
                    **轮次总体评分：** [0-100分]
                    
                    **轮次优势分析：strengths**
                    [详细分析该轮次中表现突出的方面，采用MarkDown格式]
                    
                    **轮次不足分析：improvements**
                    [详细深入指出该轮次中存在的问题，采用MarkDown格式]
                    
                    **轮次学习建议：suggestions**
                    [针对该轮次表现的详细具体学习建议，采用MarkDown格式]
                    
                    **综合评价：evaluation**
                    [详细深入综合评价，采用MarkDown格式]
                    
                    请确保评价客观、专业，既要肯定优势，也要指出不足，并提供建设性的改进建议。
                    """,
                roundDO.getRoundNumber(),
                roundDO.getRoundType(),
                roundDO.getDescription(),
                roundDO.getTotalQuestion(),
                answeredCount,
                stats.getAverageScore(),
                stats.getQuestionsInfo());
    }
}
