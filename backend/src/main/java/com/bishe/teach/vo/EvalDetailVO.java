package com.bishe.teach.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EvalDetailVO {
    private Long              recordId;
    private BigDecimal        totalScore;
    private BigDecimal        choiceScore;
    private BigDecimal        essayScore;
    private String            essayContent;
    private LocalDateTime     createTime;
    private List<AnswerItemVO> answers;

    @Data
    public static class AnswerItemVO {
        private Long       questionId;
        private Integer    questionNo;
        private String     questionContent;
        private Integer    answerValue;
        private String     answerLabel;
        private BigDecimal score;
        private BigDecimal maxScore;
    }
}
