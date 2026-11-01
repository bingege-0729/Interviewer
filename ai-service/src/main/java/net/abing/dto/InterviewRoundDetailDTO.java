package net.abing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 面试轮次详情DTO（用于详情接口返回）
 * 包含轮次基本信息 + 该轮次下的题目列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundDetailDTO {

    private Long id;

    /** 第几轮面试 */
    private Integer roundNumber;

    /** 本轮面试类型 */
    private String roundType;

    /** 本轮面试的主要考察内容 */
    private String description;

    /** 面试状态 */
    private String status;

    /** 面试总题目数量 */
    private Integer totalQuestion;

    /** 已经回答的题目 */
    private Integer answeredQuestions;

    /** 总体评分 */
    private BigDecimal overallScore;

    /** 面试总结和评价 */
    private String summary;

    /** 优势分析 */
    private String strength;

    /** 需要改进的地方 */
    private String improvement;

    /** 具体建议 */
    private String suggestion;

    /** 该轮次下的题目列表 */
    private List<QuestionDTO> questions;
}
