package com.bishe.teach.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TeacherEvalStatsVO {
    private Long       teacherId;
    private String     realName;
    private String     department;
    private String     title;
    private BigDecimal avgScore;
    private Integer    evalCount;
    private Integer    excludedCount;
    private BigDecimal avgChoiceScore;
    private BigDecimal avgEssayScore;
    private List<String> courses;
    private String     classes;
}
