package com.bishe.teach.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        username;
    @JsonIgnore
    private String        password;
    private String        realName;
    private String        email;
    private String        phone;
    private String        avatar;
    /** 角色类型: 1=学生 2=教师 3=管理员 */
    private Integer       roleType;
    /** 状态: 0=禁用 1=启用 */
    private Integer       status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer       deleted;
}
