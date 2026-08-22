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

set "TARGET_REF=%~1"
if "%TARGET_REF%"=="" set "TARGET_REF=%PARAM_BRANCH%"

if not exist "%REPO_DIR%" (
  if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
  pushd "%BUILD_DIR%"
  if defined TARGET_REF (
    echo Cloning repository with branch/tag: %TARGET_REF% ...
    git clone -b "%TARGET_REF%" "%REPO_URL%" "%APP_NAME%"
    if %errorlevel% neq 0 (
      echo [ERROR] Failed to clone branch or tag '%TARGET_REF%' from %REPO_URL%.
      popd
      exit /b 1
    )
  ) else (
    echo Cloning repository from %REPO_URL% ...
    git clone "%REPO_URL%" "%APP_NAME%"
    if %errorlevel% neq 0 (
      echo [ERROR] Failed to clone repository from %REPO_URL%.
      popd
      exit /b 1
    )
  )
  popd
) else (
  pushd "%REPO_DIR%"
  echo Fetching latest changes and tags from remote repository...
  git fetch --all --tags --prune
  if %errorlevel% neq 0 (
    echo [ERROR] Failed to fetch updates from remote repository.
    popd
    exit /b 1
  )
  if defined TARGET_REF (
    echo Switching to branch or tag: %TARGET_REF% ...
    git rev-parse --verify --quiet "refs/tags/%TARGET_REF%" >nul 2>nul
    if %errorlevel% equ 0 (
      echo Checking out tag '%TARGET_REF%'...
      git checkout -q "refs/tags/%TARGET_REF%"
    ) else (
      git rev-parse --verify --quiet "refs/heads/%TARGET_REF%" >nul 2>nul
      if %errorlevel% equ 0 (
        echo Checking out local branch '%TARGET_REF%'...
        git checkout -q "%TARGET_REF%"
        git rev-parse --verify --quiet "origin/%TARGET_REF%" >nul 2>nul
        if %errorlevel% equ 0 (
          git pull --ff-only origin "%TARGET_REF%" 2>nul || git pull origin "%TARGET_REF%"
        )
      ) else (
        git rev-parse --verify --quiet "origin/%TARGET_REF%" >nul 2>nul
        if %errorlevel% equ 0 (
          echo Checking out remote branch 'origin/%TARGET_REF%'...
          git checkout -B "%TARGET_REF%" "origin/%TARGET_REF%"
        ) else (
          git rev-parse --verify --quiet "%TARGET_REF%^{commit}" >nul 2>nul
          if %errorlevel% equ 0 (
            echo Checking out commit '%TARGET_REF%'...
            git checkout -q "%TARGET_REF%"
          ) else (
            echo [ERROR] Branch, tag, or commit '%TARGET_REF%' not found in repository.
            echo [ERROR] Please check the branch or tag name and try again.
            popd
            exit /b 1
          )
        )
      )
    )
  ) else (
    echo Pulling latest changes for current branch...
    git pull
  )
  popd
)
