@echo off
chcp 65001
:: ==================== 【仅需修改这里的仓库地址】 ====================
set GIT_REPO_URL=https://github.com/tianwy1994/child-learning-robot-firmware.git
set REMOTE_NAME=github
set BRANCH=test  :: 目标分支改为test
set COMMIT_MSG=feat: 新增功能
:: ===================================================================

echo 开始执行 Git 推送流程...
echo.

:: 初始化git仓库（若未初始化）
git init >nul 2>&1

:: 1. 移除旧同名远程（避免重复添加报错）
git remote remove %REMOTE_NAME% 2>nul

:: 2. 添加远程仓库
git remote add %REMOTE_NAME% %GIT_REPO_URL% >nul 2>&1
echo 已添加/更新远程仓库：%GIT_REPO_URL%
echo.

:: 3. 拉取远程所有分支信息（确保能检测到远程test分支）
git fetch %REMOTE_NAME% >nul 2>&1

:: 4. 检测本地是否存在test分支
git rev-parse --verify %BRANCH% >nul 2>&1
if %errorlevel% equ 0 (
    echo 本地已存在%BRANCH%分支，切换到该分支
    git checkout %BRANCH% >nul 2>&1
) else (
    echo 本地不存在%BRANCH%分支，创建并切换到%BRANCH%分支
    git checkout -b %BRANCH% >nul 2>&1
)

:: 5. 检测远程是否存在test分支，若存在则与远程同步
git ls-remote --heads %REMOTE_NAME% %BRANCH% >nul 2>&1
if %errorlevel% equ 0 (
    echo 远程已存在%BRANCH%分支，拉取最新代码
    git pull %REMOTE_NAME% %BRANCH% >nul 2>&1
)

:: 6. 暂存所有文件
git add .
echo 已执行 git add .
echo.

:: 7. 提交代码（若有变更才提交，避免空提交报错）
git diff --quiet --exit-code HEAD || (
    git commit -m "%COMMIT_MSG%"
    echo 已执行提交：%COMMIT_MSG%
    echo.
)

:: 8. 推送到远程test分支（若远程无test分支则自动创建）
git push %REMOTE_NAME% %BRANCH%
echo 已推送到远程%BRANCH%分支
echo.
echo ==================== 执行完成 ====================
pause