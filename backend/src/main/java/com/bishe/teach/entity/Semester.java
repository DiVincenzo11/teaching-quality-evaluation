package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("semester")
public class Semester {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        semesterName;
    private LocalDate     startDate;
    private LocalDate     endDate;
    /** 是否当前学期: 0=否 1=是 */
    private Integer       isCurrent;
    /** 是否允许评价: 0=禁止 1=允许 */
    private Integer       allowEvaluation;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
