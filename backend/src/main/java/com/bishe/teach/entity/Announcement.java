package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        title;
    private String        content;
    private Long          publisherId;
    /** 状态: 0=草稿 1=已发布 */
    private Integer       status;
    /** 置顶: 0=否 1=是 */
    private Integer       topFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer       deleted;
}
