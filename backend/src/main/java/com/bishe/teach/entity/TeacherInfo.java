package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teacher_info")
public class TeacherInfo {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          userId;
    private String        teacherNo;
    private String        department;
    private String        title;
    private String        courses;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
