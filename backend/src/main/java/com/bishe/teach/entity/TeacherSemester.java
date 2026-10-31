package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teacher_semester")
public class TeacherSemester {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          teacherId;
    private Long          semesterId;
    private String        courseName;
    private Integer       classStart;
    private Integer       classEnd;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
