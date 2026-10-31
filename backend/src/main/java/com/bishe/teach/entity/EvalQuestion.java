package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eval_question")
public class EvalQuestion {

    @TableId(type = IdType.AUTO)
    private Long          id;
    /** 题目编号 1-10 */
    private Integer       questionNo;
    /** 题型: 1=单选 2=简答 */
    private Integer       questionType;
    private String        questionContent;
    /** 满分 */
    private BigDecimal    maxScore;
    private Integer       sortOrder;
    private Integer       status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
