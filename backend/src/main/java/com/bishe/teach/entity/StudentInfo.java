package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_info")
public class StudentInfo {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          userId;
    private String        studentNo;
    private String        className;
    private String        major;
    private String        grade;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
