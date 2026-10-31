#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成学生评价数据脚本
为每个学生在每个学期对每位任课教师生成评价（评分+评语）
- 教师评分：按正态分布，覆盖 0-60、60-70、70-80、80-90、90-100 五个区间
- 学生出勤率、课后用时：各有差异，符合真实分布
执行前需确保：schema.sql, data.sql, new_teachers.sql, add_students.sql, add_sw_classes.sql 已执行
"""
import os
import re
import random
from typing import List, Tuple, Optional, Dict

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    exit(1)

# 数据库配置（可从环境变量覆盖）
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "teach_eval"),
    "charset": "utf8mb4",
}

# 选项得分比例 1->0%, 2->25%, 3->50%, 4->75%, 5->100%
OPTION_RATIO = {1: 0.0, 2: 0.25, 3: 0.5, 4: 0.75, 5: 1.0}
MAX_CHOICE_SCORE = 7.5
MAX_ESSAY_SCORE = 10.0

STUDENT_VARIANCE = 5  # 同一教师下不同学生评分的波动

# 出勤率/课后用时：1-5 档，正态分布（多数 3-4，少数 1 或 5）
# 权重近似：1->8%, 2->18%, 3->38%, 4->28%, 5->8%
ATTEND_STUDY_WEIGHTS = [8, 18, 38, 28, 8]

# 评语模板：含优质词(好评)、中性词(中评)、贬义词(差评)，便于语义分析分类
# 好评：含正面关键词
ESSAY_TEMPLATES_HIGH = [
    "老师讲课认真负责，很有耐心，讲解清晰易懂。",
    "教师专业严谨，课堂生动有趣，对学生很用心。",
    "老师备课充分，授课热情，能启发学生思考。",
    "教学态度认真，讲解深入浅出，非常优秀。",
    "老师负责耐心，课程内容丰富，收获很大。",
]
# 中评：仅含中性词（一般、尚可、还好、中规中矩、正常、合格、基本、平稳、还行），不含正面词
ESSAY_TEMPLATES_MID = [
    "整体一般，课程内容尚可。",
    "教学效果还好，中规中矩。",
    "老师讲课正常水平，基本合格。",
    "课程安排平稳，还行。",
    "教学还可以，基本能听懂。",
    "整体表现普通，尚可接受。",
    "课程安排正常，平稳进行。",
]
# 差评：含贬义词（枯燥、敷衍、不认真、差劲、乏味等）
ESSAY_TEMPLATES_LOW = [
    "讲课有些枯燥，希望能改进。",
    "教学态度敷衍，不够认真。",
    "课程内容乏味，听不进去。",
    "老师讲课照本宣科，比较枯燥。",
    "教学态度不认真，有待提高。",
    "课程安排有些拖沓，效果一般。",
    "老师讲课比较应付，不够用心。",
]


def extract_class_num(class_name: str) -> Optional[int]:
    """从班级名提取班号：软件工程1班->1, 计算机科学与技术1班->1"""
    if not class_name:
        return None
    m = re.search(r"(\d+)班", class_name)
    return int(m.group(1)) if m else None


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def fetch_students(conn) -> List[Tuple[int, int]]:
    """返回 [(user_id, class_num), ...]"""
    cur = conn.cursor()
    cur.execute("""
        SELECT s.user_id, s.class_name
        FROM student_info s
        JOIN sys_user u ON u.id = s.user_id AND u.deleted = 0
    """)
    rows = cur.fetchall()
    cur.close()
    result = []
    for user_id, class_name in rows:
        cn = extract_class_num(class_name)
        if cn is not None:
            result.append((user_id, cn))
    return result


def fetch_semesters(conn) -> List[Tuple[int, str]]:
    """返回 [(semester_id, semester_name), ...] 仅前6个学期"""
    cur = conn.cursor()
    cur.execute("""
        SELECT id, semester_name FROM semester
        WHERE semester_name IN (
            '2022-2023学年第一学期', '2022-2023学年第二学期',
            '2023-2024学年第一学期', '2023-2024学年第二学期',
            '2024-2025学年第一学期', '2024-2025学年第二学期'
        )
        ORDER BY id
    """)
    rows = cur.fetchall()
    cur.close()
    return rows


def fetch_teacher_semester(conn) -> List[Tuple[int, int, int, int]]:
    """返回 [(teacher_id, semester_id, class_start, class_end), ...] 去重"""
    cur = conn.cursor()
    cur.execute("""
        SELECT ts.teacher_id, ts.semester_id, ts.class_start, ts.class_end
        FROM teacher_semester ts
        JOIN sys_user u ON u.id = ts.teacher_id AND u.deleted = 0
        GROUP BY ts.teacher_id, ts.semester_id, ts.class_start, ts.class_end
    """)
    rows = cur.fetchall()
    cur.close()
    # 同一教师同一学期可能有多条（不同课程），取 class_start/class_end 的并集
    by_teacher_semester = {}
    for tid, sid, cs, ce in rows:
        key = (tid, sid)
        if key not in by_teacher_semester:
            by_teacher_semester[key] = (cs, ce)
        else:
            old_cs, old_ce = by_teacher_semester[key]
            by_teacher_semester[key] = (min(old_cs, cs), max(old_ce, ce))
    return [(tid, sid, cs, ce) for (tid, sid), (cs, ce) in by_teacher_semester.items()]


def fetch_question_ids(conn) -> List[int]:
    """返回单选题的 question_id 列表（1-12题）"""
    cur = conn.cursor()
    cur.execute("SELECT id FROM eval_question WHERE question_type = 1 ORDER BY question_no")
    rows = cur.fetchall()
    cur.close()
    return [r[0] for r in rows]


def _weights_for_ratio(ratio: float) -> List[int]:
    """根据目标得分率(0~1)返回选项1-5的权重，使期望得分率≈ratio
    选项1->0%, 2->25%, 3->50%, 4->75%, 5->100%"""
    ratio = max(0.05, min(0.98, ratio))
    if ratio < 0.2:
        return [50, 35, 12, 2, 1]  # 低分：多数1、2
    elif ratio < 0.35:
        return [25, 40, 28, 5, 2]
    elif ratio < 0.5:
        return [15, 30, 40, 12, 3]
    elif ratio < 0.75:
        return [3, 12, 35, 40, 10]
    elif ratio < 0.9:
        return [1, 3, 12, 50, 34]
    else:
        return [0, 0, 3, 30, 67]  # 高分：多数4、5，期望约90%


def gen_choice_answers(question_ids: List[int], target_choice_score: float) -> Tuple[List[Tuple[int, int, float]], float]:
    """根据目标单选题总分生成12道答案，使总分在 target 附近波动"""
    ratio = max(0.05, min(0.98, target_choice_score / 90.0))
    weights = _weights_for_ratio(ratio)
    answers = []
    total = 0.0
    for qid in question_ids:
        val = random.choices([1, 2, 3, 4, 5], weights=weights, k=1)[0]
        score = round(OPTION_RATIO[val] * MAX_CHOICE_SCORE, 2)
        total += score
        answers.append((qid, val, score))
    return answers, round(total, 2)


def gen_essay(target_essay_score: float) -> Tuple[str, float]:
    """根据目标简答题得分生成评语，确保好评/中评/差评均有分布
    - 高分教师：以好评为主，适量中评，少量差评
    - 低分教师：中评和差评相应增多"""
    target_essay_score = max(0, min(10, target_essay_score))
    if target_essay_score >= 7.5:
        # 高分：70%好评 25%中评 5%差评（确保每位老师都有中评）
        templates = random.choices(
            [ESSAY_TEMPLATES_HIGH, ESSAY_TEMPLATES_MID, ESSAY_TEMPLATES_LOW],
            weights=[70, 25, 5], k=1
        )[0]
        text = random.choice(templates)
        score = round(random.uniform(max(7.5, target_essay_score - 1.5), min(10, target_essay_score + 0.5)), 2)
    elif target_essay_score >= 5:
        # 中分：15%好评 70%中评 15%差评
        templates = random.choices(
            [ESSAY_TEMPLATES_HIGH, ESSAY_TEMPLATES_MID, ESSAY_TEMPLATES_LOW],
            weights=[15, 70, 15], k=1
        )[0]
        text = random.choice(templates)
        score = round(random.uniform(max(5, target_essay_score - 1.5), min(7.5, target_essay_score + 0.5)), 2)
    else:
        # 低分：5%好评 35%中评 60%差评（中评和差评明显增多）
        templates = random.choices(
            [ESSAY_TEMPLATES_HIGH, ESSAY_TEMPLATES_MID, ESSAY_TEMPLATES_LOW],
            weights=[5, 35, 60], k=1
        )[0]
        text = random.choice(templates)
        score = round(random.uniform(max(0, target_essay_score - 1), min(5, target_essay_score + 1)), 2)
    return text, min(MAX_ESSAY_SCORE, max(0, round(score, 2)))


def build_combinations(students, semesters, teacher_semester) -> List[Tuple[int, int, int]]:
    """(student_id, teacher_id, semester_id) 可评价组合"""
    combos = []
    for user_id, class_num in students:
        for tid, sid, cs, ce in teacher_semester:
            if cs <= class_num <= ce:
                combos.append((user_id, tid, sid))
    return combos


def build_teacher_targets(teacher_semester: List) -> Dict[Tuple[int, int], float]:
    """为每位教师每学期分配目标平均分，确保五个区间(0-60,60-70,70-80,80-90,90-100)均有分布"""
    seen = set()
    keys = []
    for tid, sid, cs, ce in teacher_semester:
        key = (tid, sid)
        if key not in seen:
            seen.add(key)
            keys.append(key)
    # 分层：70-80最多 > 80-90其次 > 90-100再其次 > 60-70再再其次 > 0-60最少
    ranges = [(50, 58), (62, 70), (75, 82), (83, 89), (95, 99)]
    weights = [5, 8, 42, 25, 20]
    targets = {}
    for i, key in enumerate(keys):
        r = random.choices(range(5), weights=weights, k=1)[0]
        lo, hi = ranges[r]
        targets[key] = round(random.uniform(lo, hi), 2)
    return targets


def build_student_attrs(student_ids: List[int]) -> Dict[int, Tuple[int, int]]:
    """为每位学生分配出勤率、课后用时(1-5)，各有差异"""
    attrs = {}
    for uid in student_ids:
        ar = random.choices([1, 2, 3, 4, 5], weights=ATTEND_STUDY_WEIGHTS, k=1)[0]
        st = random.choices([1, 2, 3, 4, 5], weights=ATTEND_STUDY_WEIGHTS, k=1)[0]
        attrs[uid] = (ar, st)
    return attrs


def main():
    print("连接数据库...")
    conn = get_connection()
    try:
        students = fetch_students(conn)
        semesters = fetch_semesters(conn)
        teacher_semester = fetch_teacher_semester(conn)
        question_ids = fetch_question_ids(conn)
    except Exception as e:
        print(f"查询失败: {e}")
        conn.close()
        return
    semester_ids = {s[1]: s[0] for s in semesters}
    combos = build_combinations(students, semesters, teacher_semester)
    teacher_targets = build_teacher_targets(teacher_semester)
    student_ids = list({c[0] for c in combos})
    student_attrs = build_student_attrs(student_ids)
    print(f"学生数: {len(students)}, 学期数: {len(semesters)}, 教师-学期记录: {len(teacher_semester)}")
    print(f"待生成评价组合: {len(combos)}")
    print("教师目标分: 分层分布 0-60/60-70/70-80/80-90/90-100")

    cur = conn.cursor()
    # 清空旧数据以便重新生成
    cur.execute("DELETE FROM eval_answer")
    cur.execute("DELETE FROM eval_record")
    conn.commit()
    print("已清空旧评价数据")

    inserted = 0
    for i, (student_id, teacher_id, semester_id) in enumerate(combos):
        if (i + 1) % 500 == 0:
            print(f"  已处理 {i + 1}/{len(combos)} ...")
        try:
            target_avg = teacher_targets.get((teacher_id, semester_id), 75)
            target_score = target_avg + random.gauss(0, STUDENT_VARIANCE)
            target_score = max(50, min(98, target_score))
            target_choice = target_score * 0.9
            target_essay = target_score - target_choice
            target_essay = max(0, min(10, target_essay))
            target_choice = target_score - target_essay

            choice_answers, choice_score = gen_choice_answers(question_ids, target_choice)
            essay_text, essay_score = gen_essay(target_essay)
            total_score = round(choice_score + essay_score, 2)

            ar, st = student_attrs.get(student_id, (4, 4))

            cur.execute(
                """INSERT INTO eval_record (student_id, teacher_id, semester_id, total_score, choice_score, essay_score, essay_content, status, attendance_rate, study_time)
                   VALUES (%s, %s, %s, %s, %s, %s, %s, 1, %s, %s)""",
                (student_id, teacher_id, semester_id, total_score, choice_score, essay_score, essay_text, ar, st),
            )
            record_id = cur.lastrowid
            for qid, val, score in choice_answers:
                cur.execute(
                    "INSERT INTO eval_answer (record_id, question_id, answer_value, score) VALUES (%s, %s, %s, %s)",
                    (record_id, qid, val, score),
                )
            inserted += 1
        except pymysql.IntegrityError as e:
            if "Duplicate entry" in str(e):
                pass  # 已存在则跳过
            else:
                raise
    conn.commit()

    # 确保每位教师至少有一条中评：对没有中评的教师-学期，随机选一条评语改为中评
    cur.execute("SELECT word FROM eval_keyword WHERE word_type = 2")
    neu_words = [r[0] for r in cur.fetchall()]
    cur.execute("""
        SELECT teacher_id, semester_id FROM eval_record
        WHERE essay_content IS NOT NULL AND essay_content != '' AND status = 1
        GROUP BY teacher_id, semester_id
    """)
    for tid, sid in cur.fetchall():
        cur.execute(
            "SELECT id, essay_content FROM eval_record WHERE teacher_id=%s AND semester_id=%s AND status=1 AND essay_content IS NOT NULL AND essay_content != ''",
            (tid, sid),
        )
        rows = cur.fetchall()
        has_neu = any(any(w in (c or "") for w in neu_words) for _, c in rows)
        if not has_neu and rows:
            rec_id = random.choice(rows)[0]
            mid_text = random.choice(ESSAY_TEMPLATES_MID)
            cur.execute("UPDATE eval_record SET essay_content=%s WHERE id=%s", (mid_text, rec_id))
    conn.commit()
    print("已确保每位教师至少有一条中评")

    cur.close()
    conn.close()
    print(f"完成！共插入 {inserted} 条评价记录。")


if __name__ == "__main__":
    main()
