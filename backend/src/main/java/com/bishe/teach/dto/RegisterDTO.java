package com.bishe.teach.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 30, message = "账号长度4-30位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "姓名不超过20个字")
    private String realName;

    /** 角色: 1=学生 2=教师 */
    @NotNull(message = "请选择注册角色")
    private Integer roleType;

    private String email;
    private String phone;

    // ===== 教师额外信息 =====
    private String teacherNo;
    private String department;
    private String title;
    private String courses;

    // ===== 学生额外信息 =====
    private String studentNo;
    private String className;
    private String major;
    private String grade;
}
