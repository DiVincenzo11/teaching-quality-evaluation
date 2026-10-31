package com.bishe.teach.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生查看自己某次评价的作答详情
 */
@Data
public class StudentEvalRecordDetailVO {
    private Long          recordId;
    private Long          teacherId;
    private String        teacherName;
    private Long          semesterId;
    private String        semesterName;
    private BigDecimal   totalScore;
    private BigDecimal   choiceScore;
    private BigDecimal   essayScore;
    private String       essayContent;
    private LocalDateTime createTime;
    /** 各题作答：题号、题目、选项值、选项文字、得分 */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Integer   questionNo;
        private String    questionContent;
        private Integer   answerValue;
        private String    answerLabel;  // 非常不符合/不符合/一般/符合/非常符合
        private BigDecimal score;
    }
}
