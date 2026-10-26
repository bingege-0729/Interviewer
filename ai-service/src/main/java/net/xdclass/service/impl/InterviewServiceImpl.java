package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.agent.AnswerEvaluationAgent;
import net.xdclass.component.agent.InterviewFinishAgent;
import net.xdclass.component.agent.InterviewGenerationOrchestrator;
import net.xdclass.controller.req.AnswerReq;
import net.xdclass.controller.req.InterviewCreateReq;
import net.xdclass.controller.req.InterviewFinishReq;
import net.xdclass.controller.req.InterviewPageReq;
import net.xdclass.dto.*;
import net.xdclass.enums.*;
import net.xdclass.feign.AccountBenefitFeign;
import net.xdclass.interceptor.AccountLoginInterceptor;
import net.xdclass.mapper.InterviewMapper;
import net.xdclass.mapper.InterviewRoundMapper;
import net.xdclass.mapper.QuestionMapper;
import net.xdclass.mapper.ResumeMapper;
import net.xdclass.model.InterviewDO;
import net.xdclass.model.InterviewRoundDO;
import net.xdclass.model.QuestionDO;
import net.xdclass.model.ResumeDO;
import net.xdclass.req.BenefitCheckReq;
import net.xdclass.service.BenefitTaskService;
import net.xdclass.service.InterviewService;
import net.xdclass.strategy.BenefitMessageStrategy;
import net.xdclass.strategy.BenefitMessageStrategyFactory;
import net.xdclass.util.CommonUtil;
import net.xdclass.util.JsonData;
import net.xdclass.util.JsonUtil;
import net.xdclass.util.SpringBeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * AI面试服务实现类
 * <p>
 * 核心职责：
 * 1. 面试创建：权益校验 + MQ异步触发
 * 2. 面试生成：SequentialAgent编排(简历解析→轮次生成) + ParallelAgent并行题目生成
 * 3. 答题评估：异步AI评分 + 自动触发轮次评价
 * 4. 面试结束：汇总评估 + 生成综合报告
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewMapper interviewMapper;
    private final InterviewRoundMapper interviewRoundMapper;
    private final QuestionMapper questionMapper;
    private final ResumeMapper resumeMapper;

    private final AccountBenefitFeign accountBenefitFeign;

    private final BenefitTaskService benefitTaskService;

    private final InterviewGenerationOrchestrator orchestrator;


    private final AnswerEvaluationAgent answerEvaluationAgent;

    private final InterviewFinishAgent interviewFinishAgent;

    private final BenefitMessageStrategyFactory strategyFactory;

    @Override
    public JsonData create(InterviewCreateReq req) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        //查找简历
        ResumeDO resumeDO = resumeMapper.selectById(req.getResumeId());
        if (resumeDO == null) {
            return JsonData.buildError("简历不存在");
        }
        //创建面试记录
        String interviewTitle = resumeDO.getFilename() + "_" +
                String.format("岗位:%s-%s-期望薪资:%s",
                        req.getTargetPosition(),
                        BenefitEnum.valueOf(req.getInterviewType()).getTitle(),
                        req.getExpectedSalary());
        String description = String.format("面试轮次：%s ;当前 %s 经验面试；期望薪资:%s; 期望工作城市:%s;",
                req.getInterviewType(), req.getWorkYears(), req.getExpectedSalary(), req.getTargetCity());

        String profileJson = JsonUtil.obj2Json(req);

        InterviewDO interviewDO = InterviewDO.builder()
                .resumeId(req.getResumeId())
                .accountId(accountDTO.getId())
                .title(interviewTitle)
                .description(description)
                .profile(profileJson)
                .type(req.getInterviewType())
                .status(InterviewStatusEnum.GENERATING.getCode())
                .extendContent(req.getSpecialContent())
                .profile(profileJson)
                .build();
        interviewMapper.insert(interviewDO);
        log.info("创建面试记录成功，面试ID：{}", interviewDO.getId());

        Long interviewId = interviewDO.getId();
        String businessId = interviewId.toString();

        BenefitCheckReq benefitCheckReq = BenefitCheckReq.builder()
                .benefitCode(req.getInterviewType())
                .businessId(businessId)
                .accountId(accountDTO.getId())
                .count(1)
                .build();

        JsonData jsonData = accountBenefitFeign.checkAndDeductBenefit(benefitCheckReq);
        if (jsonData.getCode() != 0) {
            log.info("权益扣减失败，面试ID：{}", interviewId);
            interviewMapper.deleteById(interviewId);
            return JsonData.buildResult(BizCodeEnum.BENEFIT_NOT_ENOUGH);
        }
        BenefitCheckResultDTO benefitCheckResultDTO = jsonData.getData(BenefitCheckResultDTO.class);

        log.info("权益扣减成功，面试ID：{}", interviewId);

        //发送MQ消息
        try {
            BenefitMessageStrategy strategy = strategyFactory.getStrategy(benefitCheckReq.getBenefitCode());
            strategy.sendBusinessMessage(
                    benefitCheckResultDTO.getMessageId(),
                    benefitCheckResultDTO.getBusinessId(),
                    accountDTO.getId()
            );
        } catch (Exception e) {
            log.error("面试创建失败，面试ID：{}", interviewId, e);
        }


        Map<String, Object> result = new HashMap<>();
        result.put("interviewId", interviewId);
        result.put("message", "面试创建成功，请等待面试生成");
        return JsonData.buildSuccess(result);
    }

    @Override
    public void generateInterview(InterviewCreateMessageDTO messageDTO) {

        Long interviewId = messageDTO.getInterviewId();
        Long accountId = messageDTO.getAccountId();
        String messageId = messageDTO.getMessageId();
        String businessId = String.valueOf(interviewId);
        String businessType = "INTERVIEW";


        try {
            log.info("开始生成面试，面试ID：{}", interviewId);
            //查找面试记录
            InterviewDO interviewDO = interviewMapper.selectById(interviewId);
            if (interviewDO == null) {
                log.info("面试记录不存在，消息ID：{}", messageId);
                benefitTaskService.rollback(messageId, businessId, businessType, "面试记录不存在");
                return;
            }
            //从profile序列化数据
            InterviewCreateReq req = JsonUtil.json2Obj(interviewDO.getProfile(), InterviewCreateReq.class);

            //查找简历
            ResumeDO resumeDO = resumeMapper.selectById(req.getResumeId());

            //编排简历解析和轮次生成
            InterviewGenerationOrchestrator.PipelineResult pipelineResult = orchestrator.generate(resumeDO.getContent(), req);

            if (pipelineResult == null || pipelineResult.getResumeInfo() == null || pipelineResult.getRoundDTOS() == null || pipelineResult.getRoundDTOS().isEmpty()) {
                log.info("面试生成失败，消息ID：{}", messageId);
                benefitTaskService.rollback(messageId, businessId, businessType, "面试生成失败");
                return;
            }

            AIResumeInfoDTO resumeInfo = pipelineResult.getResumeInfo();
            List<AIInterviewRoundDTO> roundDTOS = pipelineResult.getRoundDTOS();
            log.info("面试生成成功，消息ID：{},轮次数量:{}", messageId, roundDTOS.size());

            updateInterviewStatus(interviewId, InterviewStatusEnum.GENERATE_ROUND);

            //保存轮次信息到数据
            List<InterviewRoundDO> saveRounds = saveInterviewRound(interviewId, accountId, roundDTOS);

            //更新状态
            updateInterviewStatus(interviewId, InterviewStatusEnum.GENERATE_QA);

            //生成题目
            Map<Long, List<QuestionDTO>> questionMap = orchestrator.generateQuestions(req, resumeInfo, saveRounds);
            if (questionMap.isEmpty()) {
                log.info("题目生成失败，消息ID：{}", messageId);
                updateInterviewStatus(interviewId, InterviewStatusEnum.FAILED_GENERATE_QA);
                benefitTaskService.rollback(messageId, businessId, businessType, "题目生成失败");
                return;
            }


            //保存题目到数据库
            for (InterviewRoundDO round : saveRounds) {
                List<QuestionDTO> questions = questionMap.get(round.getId());
                if (questions != null && !questions.isEmpty()) {
                    List<QuestionDO> questionDOList = convertToQuestionDOs(questions, round, accountId);
                    questionMapper.insertBatch(questionDOList);
                    log.info("轮次{}题目保存成功，题目数量：{}", round.getId(), questionDOList.size());
                }
            }


            updateInterviewStatus(interviewId, InterviewStatusEnum.IN_PROGRESS);

            //标记任务完成
            benefitTaskService.markFinish(messageId, businessId, businessType);
            log.info("面试生成成功，消息ID：{}", messageId);

        } catch (Exception e) {
            log.error("面试生成失败，面试ID：{}", interviewId, e);
            updateInterviewStatus(interviewId, InterviewStatusEnum.FAILED_CREATE_INTERVIEW);
            benefitTaskService.rollback(messageId, businessId, businessType, "面试生成失败");
        }


    }


    /**
     * 将 QuestionDTO 列表转换为 QuestionDO 列表
     */
    private List<QuestionDO> convertToQuestionDOs(List<QuestionDTO> questionDTOs,
                                                  InterviewRoundDO round, Long accountId) {
        List<QuestionDO> questionDOList = new ArrayList<>();
        for (QuestionDTO dto : questionDTOs) {
            QuestionDO questionDO = QuestionDO.builder()
                    .interviewId(round.getInterviewId())
                    .accountId(accountId)
                    .interviewRoundId(round.getId())
                    .status(QuestionStatusEnum.UNANSWERED.getCode())
                    .content(dto.getContent())
                    .type(dto.getType())
                    .difficulty(dto.getDifficulty())
                    .category(dto.getCategory())
                    .standardAnswer(dto.getStandardAnswer())
                    .keyPoints(dto.getKeyPoints())
                    .maxScore(dto.getMaxScore() != null ? dto.getMaxScore() : 100)
                    .timeLimit(dto.getTimeLimit())
                    .build();
            questionDOList.add(questionDO);
        }
        return questionDOList;
    }

    private List<InterviewRoundDO> saveInterviewRound(Long interviewId, Long accountId, List<AIInterviewRoundDTO> roundDTOS) {
        List<InterviewRoundDO> interviewRoundDOS = new ArrayList<>();
        for (AIInterviewRoundDTO roundDTO : roundDTOS) {
            InterviewRoundDO interviewRoundDO = InterviewRoundDO.builder()
                    .interviewId(interviewId)
                    .accountId(accountId)
                    .roundNumber(roundDTO.getRoundNumber())
                    .roundType(roundDTO.getRoundType())
                    .description(roundDTO.getDescription())
                    .totalQuestion(roundDTO.getTotalQuestion())
                    .answeredQuestions(0)
                    .status(RoundStatusEnum.IN_PROGRESS.getCode())
                    .build();
            interviewRoundDOS.add(interviewRoundDO);
        }
        interviewRoundMapper.insertBatch(interviewRoundDOS);
        log.info("保存轮次信息成功，面试ID：{}", interviewId);
        return interviewRoundDOS;
    }

    @Override
    public JsonData getInterviewStatus(Long interviewId) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        InterviewDO interviewDO = findInterviewByIdAndAccount(interviewId, accountDTO.getId());
        if (interviewDO == null) {
            return JsonData.buildError("面试记录不存在");
        }
        Map<String, Object> statusInfo = new HashMap<>();
        statusInfo.put("interviewId", interviewId);
        statusInfo.put("status", interviewDO.getStatus());
        InterviewStatusEnum statusEnum = InterviewStatusEnum.getByCode(interviewDO.getStatus());
        statusInfo.put("statusName", statusEnum != null ? statusEnum.getDesc() : "未知状态");
        return JsonData.buildSuccess(statusInfo);
    }

    @Override
    public JsonData answer(AnswerReq req) {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        //更新题目状态
        questionMapper.update(new LambdaUpdateWrapper<QuestionDO>()
                .eq(QuestionDO::getId, req.getQuestionId()).eq(QuestionDO::getAccountId, accountDTO.getId())
                .set(QuestionDO::getStatus, QuestionStatusEnum.ANSWERED.getCode()).set(QuestionDO::getUserAnswer, req.getAnswer()));

        //更新轮次答题数量
        updateRoundAnsweredCount(req.getQuestionId(), accountDTO.getId());

        //异步AI答案评估
        answerEvaluationAgent.evaluateAsync(req, accountDTO.getId());

        return JsonData.buildSuccess("答案提交成功,正在评估中");
    }

    /**
     * 更新轮次已经回答的题目
     *
     * @param questionId
     * @param id
     */
    private void updateRoundAnsweredCount(Long questionId, Long id) {


        QuestionDO questionDO = questionMapper.selectById(questionId);

        Long selectCount = questionMapper.selectCount(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getInterviewRoundId, questionDO.getInterviewRoundId())
                .eq(QuestionDO::getStatus, QuestionStatusEnum.ANSWERED.getCode()));

        interviewRoundMapper.update(new LambdaUpdateWrapper<InterviewRoundDO>()
                .eq(InterviewRoundDO::getId, questionDO.getInterviewRoundId())
                .set(InterviewRoundDO::getAnsweredQuestions, selectCount));

    }

    @Override
    public JsonData finishAsync(InterviewFinishReq req, Long accountId) {

        InterviewDO interviewDO = interviewMapper.selectOne(new LambdaQueryWrapper<InterviewDO>()
                .eq(InterviewDO::getId, req.getInterviewId()).eq(InterviewDO::getAccountId, accountId));

        if (interviewDO == null) {
            return JsonData.buildError("面试记录不存在");
        }

        //状态校验
        String status = interviewDO.getStatus();

        if (InterviewStatusEnum.EVALUATING.getCode().equals(status)) {
            return JsonData.buildSuccess("面试记录正在评估中，请稍等");
        }
        if (InterviewStatusEnum.GENERATING.getCode().equals(status)) {
            return JsonData.buildSuccess("面试记录正在生成中，请稍等");
        }
        if (InterviewStatusEnum.COMPLETED.getCode().equals(status)) {
            return JsonData.buildSuccess("面试记录已经完成");
        }
        if (InterviewStatusEnum.CANCELLED.getCode().equals(status)) {
            return JsonData.buildSuccess("面试已经取消，无法结束");
        }
        //更新面试状态
        updateInterviewStatus(req.getInterviewId(), InterviewStatusEnum.EVALUATING);

        //异步处理面试结束流程
        interviewFinishAgent.executeAsync(req.getInterviewId(), accountId);

        return JsonData.buildSuccess("面试结束流程已启动，请稍等");
    }

    @Override
    public void finishAsyncInternal(InterviewFinishReq req, Long accountId) {

    }

    @Override
    public JsonData viewInterviewDetail(Long interviewId) {

        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        InterviewDO interviewDO = findInterviewByIdAndAccount(interviewId, accountDTO.getId());

        List<InterviewRoundDO> rounds = getRoundsByInterview(interviewId, accountDTO.getId());

        List<QuestionDO> allQuestions = getQuestionsByInterview(interviewId, accountDTO.getId());

        //面试DTO组装
        InterviewDTO interviewDTO = SpringBeanUtil.copyProperties(interviewDO, InterviewDTO.class);

        //轮次构建
        Map<Long, List<QuestionDO>> questionByRound = allQuestions.stream().collect(Collectors.groupingBy(QuestionDO::getInterviewRoundId));

        List<InterviewRoundDetailDTO> roundDetailList = buildRoundDetailList(rounds, questionByRound);

        //组装返回
        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("interview", interviewDTO);
        detailMap.put("rounds", roundDetailList);
        return JsonData.buildSuccess(detailMap);
    }

    private List<InterviewRoundDetailDTO> buildRoundDetailList(List<InterviewRoundDO> rounds, Map<Long, List<QuestionDO>> questionByRound) {
        return rounds.stream().map(
                round -> {
                    InterviewRoundDetailDTO dto = SpringBeanUtil.copyProperties(round, InterviewRoundDetailDTO.class);
                    List<QuestionDO> questionDOS = questionByRound.get(round.getId());
                    if (questionDOS != null){
                        List<QuestionDTO> questionDTOS = SpringBeanUtil.copyProperties(questionDOS, QuestionDTO.class);
                        dto.setQuestions(questionDTOS);
                    }
                    return dto;
                }
        ).filter(dto -> dto.getQuestions() != null && !dto.getQuestions().isEmpty()).toList();
    }

    @Override
    public JsonData page(InterviewPageReq req) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        LambdaQueryWrapper<InterviewDO> queryWrapper = new LambdaQueryWrapper<InterviewDO>()
                .eq(InterviewDO::getAccountId, accountDTO.getId())
                .orderByDesc(InterviewDO::getGmtCreate);

        Page<InterviewDO> interviewPage = interviewMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()), queryWrapper);
        Map<String, Object> result = CommonUtil.convertToPageMap(interviewPage, InterviewDTO.class);
        return JsonData.buildSuccess(result);
    }

    // ==================== 删除面试 ====================

    @Override
    public JsonData delete(Long interviewId) {
        AccountDTO accountDTO = AccountLoginInterceptor.threadLocal.get();

        InterviewDO interviewDO = findInterviewByIdAndAccount(interviewId, accountDTO.getId());
        if (interviewDO == null) {
            return JsonData.buildError("面试记录不存在或无权限删除");
        }

        // 删除轮次和题目
        interviewRoundMapper.delete(new LambdaQueryWrapper<InterviewRoundDO>()
                .eq(InterviewRoundDO::getInterviewId, interviewId)
                .eq(InterviewRoundDO::getAccountId, accountDTO.getId()));
        questionMapper.delete(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getInterviewId, interviewId)
                .eq(QuestionDO::getAccountId, accountDTO.getId()));
        interviewMapper.deleteById(interviewId);

        log.info("删除面试记录成功，面试ID：{}", interviewId);
        return JsonData.buildSuccess("面试记录删除成功");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据ID和账号查询面试记录（通用方法，避免重复编写查询逻辑）
     */
    private InterviewDO findInterviewByIdAndAccount(Long interviewId, Long accountId) {
        return interviewMapper.selectOne(
                new LambdaQueryWrapper<InterviewDO>()
                        .eq(InterviewDO::getId, interviewId)
                        .eq(InterviewDO::getAccountId, accountId));
    }

    /**
     * 更新面试状态
     */
    private void updateInterviewStatus(Long interviewId, InterviewStatusEnum statusEnum) {
        InterviewDO updateDO = InterviewDO.builder()
                .id(interviewId)
                .status(statusEnum.getCode())
                .description(statusEnum.getDesc())
                .build();
        interviewMapper.updateById(updateDO);
        log.info("更新面试状态，面试ID：{}，状态：{}", interviewId, statusEnum.getCode());
    }

    /**
     * 查询面试下所有轮次（按轮次号升序）
     */
    private List<InterviewRoundDO> getRoundsByInterview(Long interviewId, Long accountId) {
        return interviewRoundMapper.selectList(
                new LambdaQueryWrapper<InterviewRoundDO>()
                        .eq(InterviewRoundDO::getInterviewId, interviewId)
                        .eq(InterviewRoundDO::getAccountId, accountId)
                        .orderByAsc(InterviewRoundDO::getRoundNumber)
        );
    }

    /**
     * 查询面试下所有题目（按创建时间升序）
     */
    private List<QuestionDO> getQuestionsByInterview(Long interviewId, Long accountId) {
        return questionMapper.selectList(
                new LambdaQueryWrapper<QuestionDO>()
                        .eq(QuestionDO::getInterviewId, interviewId)
                        .eq(QuestionDO::getAccountId, accountId)
                        .orderByAsc(QuestionDO::getGmtCreate)
        );
    }
}