package com.bishe.teach.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String  token;
    private Long    userId;
    private String  username;
    private String  realName;
    private Integer roleType;
    private String  avatar;
}
