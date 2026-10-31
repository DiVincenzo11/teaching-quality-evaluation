package com.bishe.teach.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简答题语义分析工具
 * 计分规则:
 *   优质评价词 >= 3 → 10分
 *   优质评价词 == 2 → 6分
 *   优质评价词 == 1 → 3分
 *   优质评价词 == 0 → 0分（含中性或无内容）
 *   出现1次及以上负面评价词 → 扣5分（最多扣5分，总分不低于0）
 */
@Slf4j
@Component
public class SemanticAnalysisUtil {

    /**
     * 计算简答题得分
     *
     * @param text         学生评语
     * @param positiveWords 优质评价词列表
     * @param negativeWords 负面评价词列表
     * @param maxScore      该题满分
     * @return 最终得分
     */
    public BigDecimal calcEssayScore(String text,
                                     List<String> positiveWords,
                                     List<String> negativeWords,
                                     BigDecimal maxScore) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        int positiveCount = countKeywords(text, positiveWords);
        boolean hasNegative = negativeWords.stream().anyMatch(text::contains);

        double baseScore;
        if (positiveCount >= 3) {
            baseScore = maxScore.doubleValue();       // 满分
        } else if (positiveCount == 2) {
            baseScore = maxScore.doubleValue() * 0.6; // 6/10 比例
        } else if (positiveCount == 1) {
            baseScore = maxScore.doubleValue() * 0.3; // 3/10 比例
        } else {
            baseScore = 0.0;
        }

        if (hasNegative) {
            baseScore = Math.max(0.0, baseScore - 5.0);
        }

        log.debug("语义分析: 文本=[{}], 优质词数={}, 负面={}, 得分={}",
                text, positiveCount, hasNegative, baseScore);

        return BigDecimal.valueOf(baseScore);
    }

    /**
     * 统计文本中关键词出现次数（同一词多次出现只计一次）
     */
    private int countKeywords(String text, List<String> keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 根据选项值计算单选题得分
     * 非常不符合(1)→0, 不符合(2)→2.5, 一般(3)→5, 符合(4)→7.5, 非常符合(5)→10
     * 按实际满分比例计算
     */
    public BigDecimal calcChoiceScore(int answerValue, BigDecimal maxScore) {
        double ratio = switch (answerValue) {
            case 1 -> 0.0;
            case 2 -> 0.25;
            case 3 -> 0.5;
            case 4 -> 0.75;
            case 5 -> 1.0;
            default -> 0.0;
        };
        return BigDecimal.valueOf(ratio * maxScore.doubleValue());
    }

    /**
     * 获取选项文字标签
     */
    public String getAnswerLabel(int answerValue) {
        return switch (answerValue) {
            case 1 -> "非常不符合";
            case 2 -> "不符合";
            case 3 -> "一般";
            case 4 -> "符合";
            case 5 -> "非常符合";
            default -> "未知";
        };
    }
}
