package com.bishe.teach.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户管理列表VO：合并 sys_user + teacher_info / student_info 扩展字段
 */
@Data
public class UserListVO {

    // ===== sys_user 字段 =====
    private Long          id;
    private String        username;
    private String        realName;
    private String        email;
    private String        phone;
    /** 1=学生 2=教师 3=管理员 */
    private Integer       roleType;
    /** 0=禁用 1=启用 */
    private Integer       status;
    private LocalDateTime createTime;

    // ===== 教师扩展字段 (roleType=2) =====
    private String        department;
    private String        title;
    private String        courses;
    private String        teacherNo;

    // ===== 学生扩展字段 (roleType=1) =====
    private String        studentNo;
    private String        className;
    private String        major;
    private String        grade;

    /** 评教状态：true=已完成（evalCount == teacherTotal），false=未完成 */
    private Boolean       evalCompleted;
    /** 当前学期已评教师数量 */
    private Integer       evalCount;
    /** 本学期该学生需评教师总数（按班级课程） */
    private Integer       teacherTotal;
}
