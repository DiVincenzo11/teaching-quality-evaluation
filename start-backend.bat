@echo off
echo 启动后端服务...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-backend.ps1"
pause
