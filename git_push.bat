@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo        UniRemote - Git Commit and Push Script
echo ========================================================
echo Target Repo: https://github.com/Livuniq/UniRemote.git
echo.

cd /d "%~dp0"

:: Check if git is installed
where git >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Git command not found! Please install Git or add it to your system PATH.
    echo.
    pause
    exit /b 1
)

:: Initialize git repository if not present
if not exist ".git" (
    echo [*] Initializing Git repository...
    git init
    echo.
)

:: Ensure remote origin is set correctly
git remote | findstr /r "^origin$" >nul
if %ERRORLEVEL% equ 0 (
    echo [*] Updating remote origin URL...
    git remote set-url origin https://github.com/Livuniq/UniRemote.git
) else (
    echo [*] Adding remote origin...
    git remote add origin https://github.com/Livuniq/UniRemote.git
)

:: Set default branch to main
git branch -M main

:: Ask for custom commit message or use default
echo.
set /p "commit_msg=Enter commit message (Press ENTER for default: 'Initial release of UniRemote Android App'): "
if "!commit_msg!"=="" set "commit_msg=Initial release of UniRemote Android App"

echo.
echo [*] Staging files (respecting .gitignore)...
git add .

echo [*] Committing changes...
git commit -m "!commit_msg!"

echo.
echo [*] Pushing to GitHub (origin main)...
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo  [SUCCESS] Code successfully pushed to GitHub!
    echo  Repo URL: https://github.com/Livuniq/UniRemote
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo  [INFO] Normal push encountered conflicts or remote commits.
    echo  Attempting to pull and rebase with remote...
    echo ========================================================
    git pull --rebase origin main
    echo [*] Retrying push...
    git push -u origin main
    if !ERRORLEVEL! equ 0 (
        echo.
        echo [SUCCESS] Push completed successfully after rebase!
    ) else (
        echo.
        echo [NOTICE] If authentication failed, please log in with your GitHub
        echo credentials (Personal Access Token or Git Credential Manager).
    )
)

echo.
pause
