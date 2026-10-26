package net.xdclass.component.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.controller.req.AnswerReq;
import net.xdclass.dto.AIAnswerDTO;
import net.xdclass.enums.QuestionStatusEnum;
import net.xdclass.mapper.QuestionMapper;
import net.xdclass.model.QuestionDO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnswerEvaluationAgent {

    private final ChatClient chatClient;

    @Autowired
    private QuestionMapper questionMapper;


    @Autowired(required = false)
    private RoundEvaluationAgent roundEvaluationAgent;


    public  AnswerEvaluationAgent(ChatClient.Builder builder){
        this.chatClient = builder.build();
    }


    @Async("aiInterviewExecutor")
    public void  evaluateAsync(AnswerReq req,Long accountId){

        log.info("开始评估答案，题目id：{}", req.getQuestionId());
        try {
            //查询题目
            QuestionDO questionDO = questionMapper.selectOne(new LambdaQueryWrapper<QuestionDO>()
                    .eq(QuestionDO::getId, req.getQuestionId()).eq(QuestionDO::getAccountId, accountId));

            if(questionDO == null){
                log.error("题目不存在，题目id：{}", req.getQuestionId());
                return;
            }

            //更新状态为评估
            questionMapper.updateById(QuestionDO.builder()
                    .id(req.getQuestionId())
                    .status(QuestionStatusEnum.EVALUATING.getCode())
                    .build());

            //构建提示词
            String prompt = buildPrompt(questionDO, req.getAnswer());


            //调用大模型进行评估
            AIAnswerDTO aiAnswerDTO = this.chatClient.prompt().user(prompt).call().entity(AIAnswerDTO.class);

            if(aiAnswerDTO == null){
                log.error("大模型评估结果为空，题目id：{}", req.getQuestionId());
                //更新为已经回答完成
                questionMapper.updateById(QuestionDO.builder()
                        .id(req.getQuestionId())
                        .userAnswer(req.getAnswer())
                        .status(QuestionStatusEnum.ANSWERED.getCode())
                        .build());
                return;
            }
            log.info("大模型评估结果：{}", aiAnswerDTO.getEvaluation());
            //更新评估结果
            questionMapper.updateById(QuestionDO.builder()
                    .id(req.getQuestionId())
                    .status(QuestionStatusEnum.COMPLETED.getCode())
                    .evaluation(aiAnswerDTO.getEvaluation())
                    .userScore(aiAnswerDTO.getScore())
                    .userAnswer(req.getAnswer())
                    .build());

            //检查是否是轮次的最后一个题目
            if(roundEvaluationAgent != null && questionDO.getInterviewRoundId() != null){
                roundEvaluationAgent.checkAndGenerate(questionDO.getInterviewRoundId(),req.getInterviewId(),accountId);
            }

        }catch (Exception e){
            log.error("评估答案异常，题目id：{}", req.getQuestionId(), e);
            try {
                //更新为已经回答完成
                questionMapper.updateById(QuestionDO.builder()
                        .id(req.getQuestionId())
                        .status(QuestionStatusEnum.ANSWERED.getCode())
                        .userAnswer(req.getAnswer())
                        .build());
            }catch (Exception ex){
                log.error("更新题目状态异常，题目id：{}，异常:{}", req.getQuestionId(), ex);
            }
        }


    }





    // AnswerEvaluationAgent 内部的提示词构建方法（6维度评分体系，总计100分）
    /**
     * 构建答案评估提示词（6维度评分体系，总计100分）
     */
    private String buildPrompt(QuestionDO questionDO, String answer) {
        return String.format("""
                    作为一名资深技术面试官，请对以下面试回答进行全面的多维度分析和评估。
                    
                    **面试题目：**
                    %s
                    
                    **应聘者回答：**
                    %s
                    
                    **参考答案：**
                    %s
                    
                    **评估要求：**
                    请从以下多个维度进行深入分析，并提供详细的评价报告：
                    
                    **1. 技术准确性分析（25分）**
                    - 技术概念理解是否正确
                    - 技术细节掌握程度
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则10分以内
                    
                    **2. 实际应用能力（20分）**
                    - 能否结合实际工作场景
                    - 解决方案的实用性
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则10分以内
                    
                    **3. 问题解决思路（20分）**
                    - 分析问题的逻辑性
                    - 解决思路的清晰度
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则10分以内
                    
                    **4. 代码质量（15分，如适用）**
                    - 代码规范性
                    - 算法效率
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则5分以内
                    
                    **5. 沟通表达能力（10分）**
                    - 表达是否清晰
                    - 逻辑结构是否合理
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则2分以内
                    
                    **6. 学习能力和潜力（10分）**
                    - 对新技术的敏感度
                    - 自我反思能力
                    - 最重要回答内容需要和题目强相关且完整内容，如果简单几个关键词或者不相干回答则2分以内
                    
                    **输出格式要求：**
                    
                    **总体评分：** [0-100分]
                    
                    **综合评价：**
                    [整体评价和职业发展建议]
                    
                    并把上述的每个要求整理出来全部内容采用markdown格式输出，存储到 evaluation 字段
                    
                    """,
                questionDO.getContent(), answer, questionDO.getStandardAnswer());
    }

}
