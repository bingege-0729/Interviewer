package net.xdclass.component.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 轮次统计信息
 */
@Getter
@AllArgsConstructor
public class RoundStatistics {
    /** 总题目数 */
    private final int totalQuestions;
    /** 已答题数 */
    private final int answeredQuestions;
    /** 已完成轮次数 */
    private final int completedRounds;
    /** 综合平均分 */
    private final BigDecimal averageScore;
}
