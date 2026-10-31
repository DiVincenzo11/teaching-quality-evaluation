# 辅助脚本

- `smoke_test.py`：用 `sql/schema.sql` 和 `sql/demo-data.sql` 初始化独立演示库后运行。`python scripts/smoke_test.py --demo-data` 检查登录、角色权限、评价、反馈和统计，会写入演示测试数据。
- `start-backend.ps1`：Windows 后端启动入口，读取根目录 `.local-env.ps1` 的本地 JDK 设置；`-CheckOnly` 只检查 Java 和 Maven。
- `generate_eval_data.py`：原毕设批量合成评价数据工具，会向配置数据库写入数据。公开仓库不含原批量学生、教师数据文件，默认启动不需要运行它。
- `check_negative_comments.py`：按关键词查询现有评语分类。

后两项可通过 `DB_HOST`、`DB_PORT`、`DB_USER`、`DB_PASSWORD` 和 `DB_NAME` 配置数据库。不要对未备份的实际数据库执行数据生成操作。
