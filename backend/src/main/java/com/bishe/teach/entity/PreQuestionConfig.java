package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("pre_question_config")
public class PreQuestionConfig {

    @TableId(type = IdType.AUTO)
    private Long       id;
    private String     questionKey;
    private Integer    optionValue;
    private BigDecimal weight;
    private String     label;
}
