package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eval_record")
public class EvalRecord {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          studentId;
    private Long          teacherId;
    private Long          semesterId;
    private BigDecimal    totalScore;
    private BigDecimal    choiceScore;
    private BigDecimal    essayScore;
    private String        essayContent;
    private Integer       status;
    /** 前置问题：出勤率 1=<50% 2=50-70% 3=70-85% 4=85-95% 5=95%以上 */
    private Integer       attendanceRate;
    /** 前置问题：课外投入 1=几乎不投入 2=<1h/周 3=1-3h/周 4=3-5h/周 5=>5h/周 */
    private Integer       studyTime;
    /** 评价权重（基于参与度+异常检测，0~1） */
    private BigDecimal    evalWeight;
    /** 是否被剔除异常 0=正常 1=已剔除 */
    private Integer       isExcluded;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
