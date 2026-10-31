-- ================================================
-- 教师教学质量评价智能分析系统 - 数据库建表脚本
-- 作者: 陈文坤 202225220207
-- ================================================

CREATE DATABASE IF NOT EXISTS teach_eval
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE teach_eval;

-- --------------------------------------------------------
-- 1. 用户表 sys_user
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号(学号/工号)',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
    real_name   VARCHAR(50)  COMMENT '真实姓名',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20)  COMMENT '手机号',
    avatar      VARCHAR(255) COMMENT '头像URL',
    role_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '角色: 1=学生 2=教师 3=管理员',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_username  (username),
    INDEX idx_role_type (role_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- --------------------------------------------------------
-- 2. 教师信息表 teacher_info
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_info (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL UNIQUE COMMENT '关联sys_user.id',
    teacher_no  VARCHAR(20)  UNIQUE COMMENT '工号',
    department  VARCHAR(100) COMMENT '所属院系',
    title       VARCHAR(50)  COMMENT '职称',
    courses     VARCHAR(500) COMMENT '所授课程(逗号分隔)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师信息表';

-- --------------------------------------------------------
-- 3. 学生信息表 student_info
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_info (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL UNIQUE COMMENT '关联sys_user.id',
    student_no  VARCHAR(20)  UNIQUE COMMENT '学号',
    class_name  VARCHAR(100) COMMENT '班级',
    major       VARCHAR(100) COMMENT '专业',
    grade       VARCHAR(20)  COMMENT '年级(如2022)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- --------------------------------------------------------
-- 4. 学期表 semester
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS semester (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    semester_name    VARCHAR(50) NOT NULL COMMENT '学期名称(如2023-2024学年第一学期)',
    start_date       DATE        COMMENT '开始日期',
    end_date         DATE        COMMENT '结束日期',
    is_current       TINYINT     DEFAULT 0 COMMENT '是否当前学期: 0=否 1=是',
    allow_evaluation TINYINT     DEFAULT 1 COMMENT '是否允许评价: 0=禁止 1=允许',
    create_time      DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

-- --------------------------------------------------------
-- 5. 评价题目表 eval_question
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS eval_question (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    question_no      INT            NOT NULL COMMENT '题目编号(1-13)',
    question_type    TINYINT        NOT NULL COMMENT '题型: 1=单选题 2=简答题',
    question_content TEXT           NOT NULL COMMENT '题目内容',
    max_score        DECIMAL(6, 2)  DEFAULT 10.00 COMMENT '该题满分',
    sort_order       INT            DEFAULT 0 COMMENT '排序号',
    status           TINYINT        DEFAULT 1 COMMENT '状态: 1=启用',
    create_time      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价题目表';

-- --------------------------------------------------------
-- 6. 语义分析关键词表 eval_keyword
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS eval_keyword (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    word        VARCHAR(50) NOT NULL UNIQUE COMMENT '关键词',
    word_type   TINYINT     NOT NULL COMMENT '类型: 1=优质 2=中性 3=负面',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_word_type (word_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语义分析关键词表';

-- --------------------------------------------------------
-- 7. 评价记录表 eval_record (每学生每教师每学期唯一)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS eval_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id   BIGINT        NOT NULL COMMENT '学生user_id',
    teacher_id   BIGINT        NOT NULL COMMENT '教师user_id',
    semester_id  BIGINT        NOT NULL COMMENT '学期ID',
    total_score  DECIMAL(6, 2) COMMENT '总分(0-100)',
    choice_score DECIMAL(6, 2) COMMENT '单选题总分',
    essay_score  DECIMAL(6, 2) COMMENT '简答题得分',
    essay_content VARCHAR(500) COMMENT '简答题内容',
    attendance_rate TINYINT COMMENT '出勤率选项1-5',
    study_time TINYINT COMMENT '课外投入选项1-5',
    eval_weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '评价权重',
    is_excluded TINYINT DEFAULT 0 COMMENT '是否剔除异常评价',
    status       TINYINT       DEFAULT 1 COMMENT '状态: 1=有效',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_eval (student_id, teacher_id, semester_id),
    INDEX idx_teacher_semester (teacher_id, semester_id),
    INDEX idx_student          (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价记录表';

-- --------------------------------------------------------
-- 8. 评价答案表 eval_answer (单选题每题一条记录)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS eval_answer (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    record_id    BIGINT        NOT NULL COMMENT '关联eval_record.id',
    question_id  BIGINT        NOT NULL COMMENT '关联eval_question.id',
    answer_value TINYINT       COMMENT '选项值: 1=非常不符合 2=不符合 3=一般 4=符合 5=非常符合',
    score        DECIMAL(5, 2) COMMENT '该题实际得分',
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_question (record_id, question_id),
    INDEX idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价答案表';

-- --------------------------------------------------------
-- 9. 教师反馈表 teacher_feedback
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_feedback (
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    teacher_id       BIGINT   NOT NULL COMMENT '教师user_id',
    semester_id      BIGINT   NOT NULL COMMENT '学期ID',
    reflection       TEXT     COMMENT '教学反思',
    improvement_plan TEXT     COMMENT '改进计划',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_semester (teacher_id, semester_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师反馈表';

-- --------------------------------------------------------
-- 11. 教师-学期-班级关联表 teacher_semester
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_semester (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    teacher_id  BIGINT       NOT NULL COMMENT '教师user_id',
    semester_id BIGINT       NOT NULL COMMENT '学期ID',
    course_name VARCHAR(200) NOT NULL COMMENT '该学期所授课程名称',
    class_start TINYINT      NOT NULL COMMENT '教学班级起始编号(如1)',
    class_end   TINYINT      NOT NULL COMMENT '教学班级结束编号(如4)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_teacher_semester  (teacher_id, semester_id),
    INDEX idx_semester_class    (semester_id, class_start, class_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师-学期-班级关联表';

-- --------------------------------------------------------
-- 10. 通知公告表 announcement
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS announcement (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title        VARCHAR(200) NOT NULL COMMENT '公告标题',
    content      TEXT         COMMENT '公告内容',
    publisher_id BIGINT       COMMENT '发布人user_id',
    status       TINYINT      DEFAULT 1 COMMENT '状态: 0=草稿 1=已发布',
    top_flag     TINYINT      DEFAULT 0 COMMENT '置顶: 0=否 1=是',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';


-- 前置问题权重配置（全新数据库初始化所必需）
CREATE TABLE IF NOT EXISTS pre_question_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_key VARCHAR(50) NOT NULL COMMENT 'attendance 或 study_time',
    option_value TINYINT NOT NULL,
    weight DECIMAL(4,3) NOT NULL,
    label VARCHAR(50),
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_opt (question_key, option_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前置问题权重配置';
