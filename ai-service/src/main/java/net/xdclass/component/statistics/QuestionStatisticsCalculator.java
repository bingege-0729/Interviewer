package net.xdclass.component.statistics;

import net.xdclass.model.QuestionDO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题目统计计算器
 * 负责计算题目相关的统计数据
 */
@Component
public class QuestionStatisticsCalculator {

    /**
     * 构建题目统计信息
     */
    public QuestionStatistics buildStatistics(List<QuestionDO> answeredQuestions) {
        StringBuilder questionsInfo = new StringBuilder();
        int totalScore = 0;

        for (QuestionDO question : answeredQuestions) {
            questionsInfo.append(String.format("题目：%s\n回答：%s\n评分：%d分\n评价：%s\n\n",
                    question.getContent(),
                    question.getUserAnswer(),
                    question.getUserScore() != null ? question.getUserScore() : 0,
                    question.getEvaluation()));
            totalScore += question.getUserScore() != null ? question.getUserScore() : 0;
        }

        double averageScore = !answeredQuestions.isEmpty()
                ? (double) totalScore / answeredQuestions.size()
                : 0.0;

        return new QuestionStatistics(questionsInfo.toString(), averageScore);
    }
}
