package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("eval_answer")
public class EvalAnswer {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private Long          recordId;
    private Long          questionId;
    /** 选项值: 1=非常不符合 2=不符合 3=一般 4=符合 5=非常符合 */
    private Integer       answerValue;
    private BigDecimal    score;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
