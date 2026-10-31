#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
查询哪些教师有好评/中评/差评（按关键词分类）
用于验证系统好评/中评/差评分类功能
"""
import os
import sys

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "teach_eval"),
    "charset": "utf8mb4",
}


def main():
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # 获取各类关键词
    cur.execute("SELECT word FROM eval_keyword WHERE word_type = 1")
    pos_words = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT word FROM eval_keyword WHERE word_type = 2")
    neu_words = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT word FROM eval_keyword WHERE word_type = 3")
    neg_words = [r[0] for r in cur.fetchall()]

    # 查询所有有评语的记录
    cur.execute("""
        SELECT er.teacher_id, u.real_name, er.semester_id, s.semester_name, er.essay_content
        FROM eval_record er
        JOIN sys_user u ON u.id = er.teacher_id
        JOIN semester s ON s.id = er.semester_id
        WHERE er.essay_content IS NOT NULL AND er.essay_content != '' AND er.status = 1
        ORDER BY er.teacher_id, er.semester_id
    """)
    rows = cur.fetchall()

    # 分类统计
    teachers_pos = {}
    teachers_neu = {}
    teachers_neg = {}
    for teacher_id, real_name, semester_id, semester_name, content in rows:
        has_neg = any(w in content for w in neg_words)
        has_pos = any(w in content for w in pos_words)
        has_neu = any(w in content for w in neu_words)
        key = (teacher_id, real_name)
        if has_neg:
            teachers_neg.setdefault(key, []).append((semester_name, content, "差评"))
        elif has_neu and not has_pos:
            teachers_neu.setdefault(key, []).append((semester_name, content, "中评"))
        else:
            teachers_pos.setdefault(key, []).append((semester_name, content, "好评"))

    cur.close()
    conn.close()

    print("=" * 60)
    print("教师评语分类统计（好评/中评/差评）")
    print("=" * 60)
    print(f"\n有差评的教师: {len(teachers_neg)} 位")
    for (tid, name), items in sorted(teachers_neg.items(), key=lambda x: x[0][0])[:10]:
        print(f"  - {name} (ID:{tid}): {len(items)} 条差评")
    if len(teachers_neg) > 10:
        print(f"  ... 等共 {len(teachers_neg)} 位")

    print(f"\n有中评的教师: {len(teachers_neu)} 位")
    for (tid, name), items in sorted(teachers_neu.items(), key=lambda x: x[0][0])[:5]:
        print(f"  - {name} (ID:{tid}): {len(items)} 条中评")

    print(f"\n有好评的教师: {len(teachers_pos)} 位")

    # 保存有差评的教师完整名单到文件（避免控制台编码乱码）
    script_dir = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(script_dir, "teachers_with_negative.txt")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("有差评的教师名单：\n")
        f.write("=" * 50 + "\n")
        for (tid, name), items in sorted(teachers_neg.items(), key=lambda x: -len(x[1])):
            f.write(f"{name} (ID:{tid}) - {len(items)} 条差评\n")
        f.write(f"\n共 {len(teachers_neg)} 位教师有差评\n")
    print(f"\n有差评教师名单已保存至: {out_path}")


if __name__ == "__main__":
    main()
