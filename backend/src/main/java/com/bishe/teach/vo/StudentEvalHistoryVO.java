package com.bishe.teach.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentEvalHistoryVO {
    private Long          teacherId;
    /** 教师工号（如 T001） */
    private String        teacherNo;
    private String        teacherName;
    private String        department;
    private String        title;
    private Long          semesterId;
    /** 学期名称（如 2023-2024学年第一学期） */
    private String        semesterName;
    /** 该学期所授课程（逗号分隔） */
    private String        courses;
    private BigDecimal    choiceScore;
    private BigDecimal    essayScore;
    private BigDecimal    totalScore;
    private String        essayContent;
    private LocalDateTime createTime;
}
