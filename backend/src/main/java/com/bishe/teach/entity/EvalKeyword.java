package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("eval_keyword")
public class EvalKeyword {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        word;
    /** 关键词类型: 1=优质 2=中性 3=负面 */
    private Integer       wordType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
