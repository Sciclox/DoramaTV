@echo off
setlocal
echo Compiling and exporting DoramaTV APKs...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_apks.ps1"
pause
