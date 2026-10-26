package net.xdclass.component.statistics;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目统计信息
 */
@Getter
@AllArgsConstructor
public class QuestionStatistics {
    /** 所有题目的答题详情字符串（传给AI生成轮次评价） */
    private final String questionsInfo;
    /** 平均分数 */
    private final double averageScore;
}