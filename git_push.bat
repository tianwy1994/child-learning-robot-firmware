@echo off
chcp 65001
:: ==================== 【仅需修改这里的仓库地址】 ====================
set GIT_REPO_URL=https://github.com/tianwy1994/child-learning-robot-firmware.git
set REMOTE_NAME=github
set BRANCH=master
set COMMIT_MSG=feat: 新增功能
:: ===================================================================

echo 开始执行 Git 推送流程...
echo.

git init

:: 1. 移除旧同名远程（避免重复添加报错）
git remote remove %REMOTE_NAME% 2>nul

:: 2. 添加远程仓库
git remote add %REMOTE_NAME% %GIT_REPO_URL%
echo 已添加远程仓库：%GIT_REPO_URL%
echo.

:: 3. 暂存所有文件
git add .
echo 已执行 git add .
echo.

:: 4. 提交代码
git commit -m "%COMMIT_MSG%"
echo 已执行提交：%COMMIT_MSG%
echo.

:: 5. 推送到远程分支
git push %REMOTE_NAME% %BRANCH%
echo.
echo ==================== 执行完成 ====================
pause