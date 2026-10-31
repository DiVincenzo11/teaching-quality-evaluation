-- Synthetic demonstration data only. Import once into a NEW database.
-- Passwords: 123456. The original demo uses plaintext password comparison.
USE teach_eval;

INSERT INTO sys_user (username,password,real_name,role_type,status) VALUES
('admin','123456','演示管理员',3,1),
('demo_teacher','123456','演示教师',2,1),
('demo_student','123456','演示学生',1,1);

INSERT INTO teacher_info (user_id,teacher_no,department,title,courses)
SELECT id,'DEMO001','演示学院','讲师','软件工程' FROM sys_user WHERE username='demo_teacher';
INSERT INTO student_info (user_id,student_no,class_name,major,grade)
SELECT id,'DEMO2022001','软件工程2班','软件工程','2022' FROM sys_user WHERE username='demo_student';
INSERT INTO semester (semester_name,start_date,end_date,is_current,allow_evaluation)
VALUES ('演示学期','2026-09-01','2027-01-31',1,1);
INSERT INTO teacher_semester (teacher_id,semester_id,course_name,class_start,class_end)
SELECT u.id,s.id,'软件工程',1,2 FROM sys_user u CROSS JOIN semester s
WHERE u.username='demo_teacher' AND s.is_current=1;

INSERT INTO pre_question_config (question_key,option_value,weight,label) VALUES
('attendance',1,0.500,'低于50%'),('attendance',2,0.700,'50%~70%'),
('attendance',3,0.850,'70%~85%'),('attendance',4,0.950,'85%~95%'),('attendance',5,1.000,'95%以上'),
('study_time',1,0.600,'几乎不投入'),('study_time',2,0.750,'每周不足1h'),
('study_time',3,0.900,'每周1~3h'),('study_time',4,0.950,'每周3~5h'),('study_time',5,1.000,'每周5h以上');

INSERT INTO eval_question (question_no, question_type, question_content, max_score, sort_order) VALUES
(1, 1, '教师备课充分，课程内容结构合理、逻辑清晰，能将复杂或抽象的知识点讲解得条理分明。', 7.50, 1),
(2, 1, '教师授课方式具有启发性，能有效结合实际案例、多媒体或板书等教学工具，激发学生的学习兴趣。', 7.50, 2),
(3, 1, '教师能将本学科的前沿知识、最新研究成果或行业动态融入课堂，有效开阔了我的学术与认知视野。', 7.50, 3),
(4, 1, '课程的教学进度安排合理，难度适中，学习负荷(包括课外阅读与作业量)与所得学分相匹配。', 7.50, 4),
(5, 1, '教师态度友善，鼓励学生在课堂上提问与探讨，并在课外(或线上)能耐心、及时地解答学生的疑惑。', 7.50, 5),
(6, 1, '课程的作业布置适量、考核标准公平透明，且教师能对作业或测验给予及时、有建设性的评价反馈。', 7.50, 6),
(7, 1, '教师为人师表、治学严谨，尊重每一位学生，能够营造平等、包容、积极向上的课堂学习氛围。', 7.50, 7),
(8, 1, '教师尊重学生个性差异，关注每位学生的学习进展，在必要时给予个性化的指导与支持。', 7.50, 8),
(9, 1, '课程不仅注重理论传授，还注重培养我的独立思考能力、解决实际问题的能力以及创新思维。', 7.50, 9),
(10, 1, '总体而言，我对该教师的教学质量感到满意，并且通过这门课程的学习，我获得了实质性的知识与能力提升。', 7.50, 10),
(11, 1, '通过本课程的学习，我掌握了该课程的核心知识与关键技能，达到了预期的学习目标。', 7.50, 11),
(12, 1, '课程激发了我对该学科的兴趣，促使我主动进行课外拓展学习与深入思考。', 7.50, 12),
(13, 2, '用不超过50个字简单评价一下这位老师：', 10.00, 13);

INSERT INTO eval_keyword (word, word_type) VALUES
('认真', 1), ('负责', 1), ('耐心', 1), ('专业', 1), ('幽默', 1),
('生动', 1), ('清晰', 1), ('细心', 1), ('热情', 1), ('优秀', 1),
('出色', 1), ('用心', 1), ('严谨', 1), ('丰富', 1), ('启发', 1),
('精彩', 1), ('渊博', 1), ('亲切', 1), ('仔细', 1), ('投入', 1),
('博学', 1), ('循循善诱', 1), ('深入浅出', 1), ('诲人不倦', 1), ('因材施教', 1);

INSERT INTO eval_keyword (word, word_type) VALUES
('一般', 2), ('普通', 2), ('还好', 2), ('尚可', 2), ('中规中矩', 2),
('正常', 2), ('合格', 2), ('基本', 2), ('平稳', 2), ('还行', 2);

INSERT INTO eval_keyword (word, word_type) VALUES
('差', 3), ('敷衍', 3), ('无聊', 3), ('迟到', 3), ('不认真', 3),
('不负责', 3), ('草率', 3), ('差劲', 3), ('应付', 3), ('枯燥', 3),
('照本宣科', 3), ('不耐烦', 3), ('糟糕', 3), ('乏味', 3), ('拖沓', 3);

INSERT INTO announcement (title,content,publisher_id,status,top_flag)
SELECT '演示系统使用说明','请使用演示账号体验12道单选题、简答评价及教师反馈。全部数据均为演示数据。',id,1,1
FROM sys_user WHERE username='admin';
