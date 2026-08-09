@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "PROJECT_DIR=e:\gitee\production_release\RuoYi-Vue-master2026-06-24"

cd /d "%PROJECT_DIR%"
if %errorlevel% neq 0 (
    echo [错误] 无法进入项目目录: %PROJECT_DIR%
    pause
    exit /b 1
)

echo ============================================
echo         GitHub 一键提交推送脚本
echo ============================================
echo.

:: 检查是否有未提交的改动
git diff --quiet
set DIFF_RC=%errorlevel%
git diff --cached --quiet
set CACHED_RC=%errorlevel%

if %DIFF_RC% equ 0 if %CACHED_RC% equ 0 (
    echo [提示] 工作区干净，没有需要提交的改动
    echo.
    echo ============================================
    echo 直接推送到 origin/master ...
    echo ============================================
    git push origin master
    if %errorlevel% equ 0 (
        echo [成功] 推送完成
    ) else (
        echo [失败] 推送失败，请检查网络或仓库状态
    )
    pause
    exit /b 0
)

:: 显示当前改动
echo [当前改动文件]
git status -s
echo.

:: 获取 commit message
set "MSG=%~1"
if "%MSG%"=="" (
    set /p MSG="请输入 commit 信息: "
)
if "%MSG%"=="" (
    set "MSG=update: %date:~0,10% %time:~0,5%"
    echo [提示] 使用默认信息: !MSG!
)
echo.

:: 执行 git add
echo ============================================
echo 1/3  git add .
echo ============================================
git add .
if %errorlevel% neq 0 (
    echo [失败] git add 执行出错
    pause
    exit /b 1
)
echo [完成] 已暂存所有改动
echo.

:: 执行 git commit
echo ============================================
echo 2/3  git commit
echo ============================================
git commit -m "!MSG!"
if %errorlevel% neq 0 (
    echo [失败] git commit 执行出错
    pause
    exit /b 1
)
echo [完成] 提交成功
echo.

:: 执行 git push
echo ============================================
echo 3/3  git push origin master
echo ============================================
git push origin master
if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo  部署成功！已推送到 GitHub
    echo ============================================
) else (
    echo.
    echo ============================================
    echo [失败] 推送失败（网络问题？），commit 已保存在本地
    echo 稍后执行: git push origin master
    echo ============================================
    pause
    exit /b 1
)
echo.
pause
