package com.bishe.teach.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuestionAvgScoreVO {
    private Long       questionId;
    private Integer    questionNo;
    private String     questionContent;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
}
