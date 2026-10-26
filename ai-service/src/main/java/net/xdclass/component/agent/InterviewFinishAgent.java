package net.xdclass.component.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.controller.req.AnswerReq;
import net.xdclass.dto.InterviewReportDTO;
import net.xdclass.enums.InterviewStatusEnum;
import net.xdclass.enums.QuestionStatusEnum;
import net.xdclass.enums.RoundStatusEnum;
import net.xdclass.mapper.InterviewMapper;
import net.xdclass.mapper.InterviewRoundMapper;
import net.xdclass.mapper.QuestionMapper;
import net.xdclass.mapper.ResumeMapper;
import net.xdclass.model.InterviewDO;
import net.xdclass.model.InterviewRoundDO;
import net.xdclass.model.QuestionDO;
import net.xdclass.model.ResumeDO;
import net.xdclass.service.ResumeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class InterviewFinishAgent {


    private final InterviewMapper interviewMapper;
    private final InterviewRoundMapper interviewRoundMapper;
    private final QuestionMapper questionMapper;
    private final ResumeMapper resumeMapper;
    private final AnswerEvaluationAgent answerEvaluationAgent;
    private final InterviewReportGenerationAgent reportGenerationAgent;
    private final RoundEvaluationAgent roundEvaluationAgent;



    @Async("aiInterviewExecutor")
    public void executeAsync(Long interviewId, Long accountId) {

        try {
            log.info("面试结束，开始处理面试数据，面试id：{}", interviewId);
            InterviewDO interviewDO = interviewMapper.selectById(interviewId);
            if (interviewDO == null) {
                log.info("面试记录不存在，面试id：{}", interviewId);
                return;
            }
            List<InterviewRoundDO> rounds =  getRoundsByInterview(interviewId,accountId);
            if (rounds.isEmpty()){
                log.error("面试轮次不存在，面试id：{}", interviewId);
                return;
            }


            //第一步：查询所有题目，和 补评 未评估的题目
            List<QuestionDO> allQuestions = getQuestionsByInterview(interviewId,accountId);
            triggerPendingEvaluations(interviewId, accountId,allQuestions);

            //第二步：等待所有评估完成
            List<Long> roundIds = rounds.stream().map(InterviewRoundDO::getId).toList();
            waitForAllEvaluations(roundIds,accountId);

            //第三步：重新查询题目 获取最新状态，再处理未做答的题目
            allQuestions = getQuestionsByInterview(interviewId,accountId);
            markUnansweredQuestions(allQuestions);

            //第四步：补触发轮次评价（解决：轮次内有未作答题目，导致 checkAndGenerate 一直不达标，轮次评价永远不会生成的问题）
            triggerRoundEvaluations(rounds, interviewId, accountId);

            //第五步：等待所有轮次评价完成
            List<Long> roundIdList = rounds.stream().map(InterviewRoundDO::getId).toList();
            waitForAllRoundEvaluations(roundIdList);

            //第六步：重新查询轮次最新数据（包含 summary/strength/improvement/suggestion），再生成综合面试评价
            rounds = getRoundsByInterview(interviewId, accountId);

            //第七步：生成综合面试评价
            ResumeDO resumeDO = resumeMapper.selectById(interviewDO.getResumeId());
            if (resumeDO == null) {
                log.error("简历不存在，无法生成面试评价，resumeId:{}", interviewDO.getResumeId());
                updateInterviewStatus(interviewId, InterviewStatusEnum.FAILED_EVALUATE_INTERVIEW);
                return;
            }

            InterviewReportDTO interviewReportDTO = reportGenerationAgent.generate(interviewDO, resumeDO.getContent(), rounds, allQuestions);

            if(interviewReportDTO != null){
                saveInterviewReport(interviewId,interviewReportDTO);
                log.info("面试评价生成成功，面试id：{}", interviewId);
            }else {
                log.error("面试评价生成失败，面试id：{}", interviewId);
                updateInterviewStatus(interviewId, InterviewStatusEnum.FAILED_EVALUATE_INTERVIEW);
            }


        }catch (Exception e){
            log.error("面试结束异常，面试id：{}", interviewId, e);
            updateInterviewStatus(interviewId, InterviewStatusEnum.FAILED_EVALUATE_INTERVIEW);
        }


    }

    private void updateInterviewStatus(Long interviewId, InterviewStatusEnum interviewStatusEnum) {

        interviewMapper.updateById(InterviewDO.builder().id(interviewId)
                .status(interviewStatusEnum.getCode()).description(interviewStatusEnum.getDesc()).build());
    }

    private void saveInterviewReport(Long interviewId, InterviewReportDTO reportDTO) {
        InterviewDO.InterviewDOBuilder builder = InterviewDO.builder()
                .id(interviewId)
                .description(InterviewStatusEnum.COMPLETED.getDesc())
                .status(InterviewStatusEnum.COMPLETED.getCode())  // 标记完成
                .overallScore(reportDTO.getOverallScore())         // 总体评分
                .summary(reportDTO.getSummary())                    // 面试总结
                .strength(reportDTO.getStrengths())                 // 优势分析
                .improvement(reportDTO.getImprovements())           // 待改进
                .suggestion(reportDTO.getSuggestions());             // 建议

        // 维度评分（空值保护）
        if (reportDTO.getDimensionScores() != null) {
            builder.technicalSkills(reportDTO.getDimensionScores().getTechnicalSkills())
                    .logicalThinking(reportDTO.getDimensionScores().getLogicalThinking())
                    .communication(reportDTO.getDimensionScores().getCommunication())
                    .problemSolving(reportDTO.getDimensionScores().getProblemSolving());
        }

        // 答题统计
        if (reportDTO.getAnswerStatistics() != null) {
            builder.passedQuestion(reportDTO.getAnswerStatistics().getPassedQuestions())
                    .excellentQuestion(reportDTO.getAnswerStatistics().getExcellentQuestions());
        }

        interviewMapper.updateById(builder.build());
        log.info("更新面试记录成功，面试ID：{}，总体评分：{}", interviewId, reportDTO.getOverallScore());
    }

    private void markUnansweredQuestions(List<QuestionDO> allQuestions) {
        List<QuestionDO> unanswered = allQuestions.stream()
                .filter(q -> !hasValidAnswer(q) && !QuestionStatusEnum.COMPLETED.getCode().equals(q.getStatus()))
                .toList();

        if (!unanswered.isEmpty()) {
            // Builder模式更新，只写入指定字段，不误覆盖其他数据
            unanswered.forEach(q -> questionMapper.updateById(QuestionDO.builder()
                    .id(q.getId())
                    .userAnswer("未作答")
                    .userScore(0)
                    .evaluation("未作答，直接评定为0分")
                    .status(QuestionStatusEnum.COMPLETED.getCode())
                    .build()));
            log.info("处理未作答题目完成，共{}道", unanswered.size());
        }
    }


    /**
     * 为每个轮次补触发轮次评价
     * <p>原 RoundEvaluationAgent.checkAndGenerate 仅在 AnswerEvaluationAgent 评估完单题后触发，
     * 当轮次内存在未作答题目时，由 markUnansweredQuestions 直接改成 COMPLETED，绕过了 AnswerEvaluationAgent，
     * 因此必须在这里主动补触发，否则轮次评价永远不会生成。</p>
     */
    private void triggerRoundEvaluations(List<InterviewRoundDO> rounds, Long interviewId, Long accountId) {
        rounds.stream()
                // 跳过已完成的轮次，避免重复生成
                .filter(r -> !RoundStatusEnum.COMPLETED.getCode().equals(r.getStatus()))
                .forEach(r -> {
                    try {
                        roundEvaluationAgent.checkAndGenerate(r.getId(), interviewId, accountId);
                    } catch (Exception e) {
                        log.error("补触发轮次评价异常，roundId:{}", r.getId(), e);
                    }
                });
    }

    /**
     * 等待所有轮次评价完成（COMPLETED 都视为终态）
     */
    private void waitForAllRoundEvaluations(List<Long> roundIds) {
        int maxWaitSeconds = 180;
        int intervalSeconds = 5;
        int waited = 0;

        while (waited < maxWaitSeconds) {
            Long pendingCount = interviewRoundMapper.selectCount(
                    new LambdaQueryWrapper<InterviewRoundDO>()
                            .in(InterviewRoundDO::getId, roundIds)
                            .notIn(InterviewRoundDO::getStatus,
                                    RoundStatusEnum.COMPLETED.getCode()));
            if (pendingCount == 0) {
                log.info("所有轮次评价已完成");
                return;
            }
            try {
                Thread.sleep(intervalSeconds * 1000L);
                waited += intervalSeconds;
                if (waited % 15 == 0) {
                    log.info("等待轮次评价完成中，已等待{}秒，剩余待评估轮次：{}", waited, pendingCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("等待轮次评价完成超时，最大等待时间：{}秒", maxWaitSeconds);
    }

    private void waitForAllEvaluations(List<Long> roundIds, Long accountId) {
        int maxWaitSeconds = 180;  // 最长等待180秒
        int intervalSeconds = 5;   // 每5秒查一次
        int waited = 0;

        while (waited < maxWaitSeconds) {
            // 查询还有多少题目处于 EVALUATING 或 ANSWERED 状态
            Long pendingCount = questionMapper.selectCount(
                    new LambdaQueryWrapper<QuestionDO>()
                            .in(QuestionDO::getInterviewRoundId, roundIds)
                            .eq(QuestionDO::getAccountId, accountId)
                            .in(QuestionDO::getStatus,
                                    QuestionStatusEnum.EVALUATING.getCode(),
                                    QuestionStatusEnum.ANSWERED.getCode()));

            if (pendingCount == 0) {
                log.info("所有评估已完成");
                return;  // 全部评估完成，退出等待
            }

            try {
                Thread.sleep(intervalSeconds * 1000L);
                waited += intervalSeconds;
                if (waited % 15 == 0) { //避免日志刷屏，又能监控进度
                    log.info("等待评估完成中，已等待{}秒，剩余待评估：{}", waited, pendingCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("等待评估完成超时，最大等待时间：{}秒", maxWaitSeconds);
    }


    private void triggerPendingEvaluations(Long interviewId, Long accountId, List<QuestionDO> allQuestions) {
        // 筛选：有有效答案 && 状态仍为ANSWERED（说明还没被评估）
        List<QuestionDO> pending = allQuestions.stream()
                .filter(q -> hasValidAnswer(q) && QuestionStatusEnum.ANSWERED.getCode().equals(q.getStatus()))
                .toList();

        if (!pending.isEmpty()) {
            log.info("发现{}道已回答但未评估的题目，触发异步评估", pending.size());
            pending.forEach(q -> answerEvaluationAgent.evaluateAsync(
                    AnswerReq.builder()
                            .questionId(q.getId())
                            .interviewId(interviewId)
                            .answer(q.getUserAnswer())  // 从userAnswer字段恢复答案
                            .build(),
                    accountId));
        }
    }

    //排除三种情况：null、空字符串、“未作答”文本
    private boolean hasValidAnswer(QuestionDO question) {
        String answer = question.getUserAnswer();
        return answer != null && !answer.trim().isEmpty() && !"未作答".equals(answer.trim());
    }

    private List<QuestionDO> getQuestionsByInterview(Long interviewId, Long accountId) {

        return questionMapper.selectList(
                new LambdaQueryWrapper<QuestionDO>()
                        .eq(QuestionDO::getInterviewId, interviewId)
                        .eq(QuestionDO::getAccountId, accountId)
        );
    }

    private List<InterviewRoundDO> getRoundsByInterview(Long interviewId, Long accountId) {

        return interviewRoundMapper.selectList(
                new LambdaQueryWrapper<InterviewRoundDO>()
                        .eq(InterviewRoundDO::getInterviewId, interviewId)
                        .eq(InterviewRoundDO::getAccountId, accountId)
                        .orderByAsc(InterviewRoundDO::getRoundNumber)
        );
    }
}
