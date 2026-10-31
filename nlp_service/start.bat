@echo off
chcp 65001
cd /d "%~dp0"
echo 正在启动教学评价 ABSA 情感分析服务...
echo.
echo 首次运行将自动下载预训练模型（约 400MB），请耐心等待
echo 服务启动后访问 http://localhost:5001/health 验证
echo.

if not exist ".venv\Scripts\python.exe" python -m venv .venv
set "HF_HOME=%~dp0..\.run\huggingface"
set "PYTHONUTF8=1"
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 goto end

".venv\Scripts\python.exe" app.py

:end
pause
