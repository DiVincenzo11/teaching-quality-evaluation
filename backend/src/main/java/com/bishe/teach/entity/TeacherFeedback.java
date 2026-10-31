package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teacher_feedback")
public class TeacherFeedback {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          teacherId;
    private Long          semesterId;
    private String        reflection;
    private String        improvementPlan;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
