package com.bishe.teach.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedbackListVO {
    private Long          id;
    private Long          teacherId;
    private String        teacherName;
    private String        title;
    private String        courseNames;
    private Long          semesterId;
    private String        semesterName;
    private String        reflection;
    private String        improvementPlan;
    private LocalDateTime updateTime;
    private java.math.BigDecimal myScore;
    private java.math.BigDecimal avgScore;
}
