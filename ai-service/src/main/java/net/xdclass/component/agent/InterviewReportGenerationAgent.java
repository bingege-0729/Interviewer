package net.xdclass.component.agent;


import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.statistics.RoundStatistics;
import net.xdclass.component.statistics.RoundStatisticsCalculator;
import net.xdclass.dto.InterviewReportDTO;
import net.xdclass.dto.QuestionDTO;
import net.xdclass.model.InterviewDO;
import net.xdclass.model.InterviewRoundDO;
import net.xdclass.model.QuestionDO;
import net.xdclass.util.SpringBeanUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 面试报告生成 Agent
 * <p>负责在面试结束后生成综合评价报告，包括：</p>
 * <ul>
 *   <li>总体评分和维度评分（技术能力、逻辑思维、沟通能力、问题解决）</li>
 *   <li>优势分析、不足分析、职业发展建议</li>
 *   <li>答题统计（及格题数、优秀题数）</li>
 * </ul>
 */
@Slf4j
@Component
public class InterviewReportGenerationAgent {

    
    private final ChatClient chatClient;
    
    private final RoundStatisticsCalculator statisticsCalculator;
    
    public InterviewReportGenerationAgent(ChatClient.Builder builder, RoundStatisticsCalculator statisticsCalculator) {
        this.chatClient = builder.build();
        this.statisticsCalculator = statisticsCalculator;
    }


    /**
     * 生成面试综合评价报告
     * @param interviewDO
     * @param resumeContent
     * @param rounds
     * @param allQuestions
     * @return
     */
    public InterviewReportDTO generate(InterviewDO interviewDO, String resumeContent,
                                       List<InterviewRoundDO> rounds,List<QuestionDO> allQuestions){
        
        try {
            //统计轮次数据
            RoundStatistics statistics = statisticsCalculator.calculate(rounds);

            //构建轮次评价信息
            String roundsInfo = statisticsCalculator.buildRoundsInfo(rounds);

            //构建提示词
            String prompt = buildPrompt(interviewDO, resumeContent, rounds, statistics, roundsInfo);

            //调用AI生成报告
            InterviewReportDTO reportDTO = this.chatClient.prompt()
                    .user(prompt).call().entity(InterviewReportDTO.class);

            if(reportDTO != null){
                long passedQuestions = allQuestions.stream()
                        .filter(q -> hasValidAnswer(q) && q.getUserScore() != null && q.getUserScore() >= 60)
                        .count();
                long excellentQuestions = allQuestions.stream()
                        .filter(q -> hasValidAnswer(q) && q.getUserScore() != null && q.getUserScore() >= 80)
                        .count();

                reportDTO.setAnswerStatistics(InterviewReportDTO.AnswerStatistics.builder()
                        .totalQuestions(statistics.getTotalQuestions())
                        .answeredQuestions(statistics.getAnsweredQuestions())
                        .averageScore(statistics.getAverageScore())
                        .passedQuestions((int) passedQuestions)
                        .excellentQuestions((int) excellentQuestions)
                        .build());
                reportDTO.setQuestionDTOList(SpringBeanUtil.copyProperties(allQuestions, QuestionDTO.class));
            }
            return reportDTO;

        }catch (Exception e){
            log.error("Error generating interview report: {}", e.getMessage());
            return null;
        }

    }

    /**
     * 判断题目是否有有效答案
     */
    private boolean hasValidAnswer(QuestionDO question) {
        String answer = question.getUserAnswer();
        return answer != null && !answer.trim().isEmpty() && !"未作答".equals(answer.trim());
    }
    
    
    
    

    private String buildPrompt(InterviewDO interviewDO, String resumeContent,
                               List<InterviewRoundDO> rounds,
                               RoundStatistics statistics, String roundsInfo) {
        return String.format("""
            作为一名资深技术面试官和人才评估专家，请根据以下面试信息生成一份完整的面试综合评价报告。
            
            **面试基本信息：**
            - 面试标题：%s
            - 面试类型：%s
            - 面试描述：%s
            - 总轮次数：%d轮
            - 已完成轮次：%d轮
            - 总题目数：%d题
            - 已作答数：%d题
            - 综合平均分：%.1f分
            
            **简历内容：**
            %s
            
            **各轮次评价信息：**
            %s
            
            **报告生成要求：**
            请从以下维度生成面试综合报告：
            
            1. **总体评分（overallScore）**：0-100分的综合评分
            
            2. **面试总结（summary）**：Markdown格式
            - 整体技术能力评估
            - 与目标岗位的匹配程度
            - 职业发展潜力判断
            
            3. **优势分析（strengths）**：Markdown格式
            - 技术层面的突出优势
            - 综合素质方面的亮点
            
            4. **待改进方面（improvements）**：Markdown格式
            - 技术短板和知识盲区
            - 需要提升的软技能
            
            5. **发展建议（suggestions）**：Markdown格式
            - 短期（1-3个月）改进建议
            - 长期（6-12个月）发展规划
            
            6. **维度评分（dimensionScores）**：
            - technicalSkills（技术能力）：0-100分
            - logicalThinking（逻辑思维）：0-100分
            - communication（沟通表达）：0-100分
            - problemSolving（解决问题）：0-100分
            """,
                interviewDO.getTitle(),
                interviewDO.getType(),
                interviewDO.getDescription(),
                rounds.size(),
                statistics.getCompletedRounds(),
                statistics.getTotalQuestions(),
                statistics.getAnsweredQuestions(),
                statistics.getAverageScore().doubleValue(),
                resumeContent,
                roundsInfo);
    }
}
