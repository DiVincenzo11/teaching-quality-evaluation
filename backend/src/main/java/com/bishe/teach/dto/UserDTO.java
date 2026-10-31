package com.bishe.teach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDTO {
    private Long    id;
    @NotBlank(message = "账号不能为空")
    private String  username;
    private String  password;
    @NotBlank(message = "姓名不能为空")
    private String  realName;
    private String  email;
    private String  phone;
    @NotNull(message = "角色类型不能为空")
    private Integer roleType;
    private Integer status;
    // 教师额外信息
    private String  teacherNo;
    private String  department;
    private String  title;
    private String  courses;
    // 学生额外信息
    private String  studentNo;
    private String  className;
    private String  major;
    private String  grade;
}
