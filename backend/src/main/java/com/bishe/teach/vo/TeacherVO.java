package com.bishe.teach.vo;

import lombok.Data;

@Data
public class TeacherVO {
    private Long   userId;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private String teacherNo;
    private String department;
    private String title;
    private String courses;
}
