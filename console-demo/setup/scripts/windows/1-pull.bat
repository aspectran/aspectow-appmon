@echo off
rem Pulls the latest source code from the Git repository.
rem If the repository is not cloned yet, it will be cloned.

rem Check if git is installed
where git >nul 2>nul
if %errorlevel% neq 0 echo Error: git is not installed. Please install it and try again.
if %errorlevel% neq 0 exit /b 1

rem Load environment variables
call "%~dp0\setenv.bat"

rem Auto-detect development mode if not explicitly set
if not defined DEV_MODE (
  if exist "%~dp0pom.xml" (
    git -C "%~dp0." rev-parse --is-inside-work-tree >nul 2>nul
    if not errorlevel 1 set "DEV_MODE=true"
  )
)

if "%DEV_MODE%"=="true" (
  echo Development environment detected.
  echo Skipping git pull in development mode to preserve local working tree.
  exit /b 0
)

set "TARGET_REPO_DIR=%REPO_DIR%"
if "%DEV_MODE%"=="true" set "TARGET_REPO_DIR=%~dp0"
set "LOCK_DIR=%TARGET_REPO_DIR%\.pull.lock"

set /a WAIT_COUNT=0
:ACQUIRE_LOCK
mkdir "%LOCK_DIR%" 2>nul
if %errorlevel% neq 0 (
    if exist "%LOCK_DIR%\success" (
        echo [PULL LOCK] Git pull was successfully completed by another node in the shared directory.
        echo [PULL LOCK] Skipping redundant git pull.
        exit /b 0
    )
    if %WAIT_COUNT% equ 0 (
        echo [PULL LOCK] Another node is currently pulling in this directory.
        echo [PULL LOCK] Waiting for active pull to complete...
    )
    set /a WAIT_COUNT+=1
    timeout /t 1 /nobreak >nul
    goto :ACQUIRE_LOCK
)

if not exist "%REPO_DIR%" (
    if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
    pushd "%BUILD_DIR%"
    git clone "%REPO_URL%" "%APP_NAME%"
    popd
    type nul > "%LOCK_DIR%\success"
    timeout /t 1 /nobreak >nul
    rmdir /s /q "%LOCK_DIR%" 2>nul
) else (
    pushd "%REPO_DIR%"
    git pull
    popd
    type nul > "%LOCK_DIR%\success"
    timeout /t 1 /nobreak >nul
    rmdir /s /q "%LOCK_DIR%" 2>nul
)
