@echo off
rem ==============================================================================
rem Application Installation and Execution Configuration for Windows
rem ==============================================================================

rem The name of the application. Used for directory names and service names.
set "APP_NAME=aspectow-console-demo"

rem The Git repository URL for the application source code.
set "REPO_URL=https://github.com/aspectran/aspectow"

rem The root directory for the application installation.
set "BASE_DIR=%~dp0"
if "%BASE_DIR:~-1%"=="\" set "BASE_DIR=%BASE_DIR:~0,-1%"

rem A temporary directory for cloning and building the application from source.
set "BUILD_DIR=%BASE_DIR%\.build"

rem The path to the cloned Git repository within the build directory.
set "REPO_DIR=%BUILD_DIR%\%APP_NAME%"

rem The directory where the runnable application files will be deployed.
set "DEPLOY_DIR=%BASE_DIR%\app"

rem A directory for backing up the previous version during an update.
set "RESTORE_DIR=%BASE_DIR%\app-restore"

rem The process name for the daemon.
set "PROC_NAME=%APP_NAME%"

set "JAVA_TOOL_OPTIONS=--enable-native-access=ALL-UNNAMED"

rem Java system properties to be passed to the Aspectran application at runtime.
set "ASPECTRAN_OPTS=-Duser.timezone=UTC -Daspectran.profiles.active=dev,gateway,console.custom-ui -Daspectran.profiles.base.console=dev,h2,console.custom-ui -Daspectran.workPath=%BASE_DIR%\app\work -Daspectran.tempPath=%BASE_DIR%\app\temp -Daspectran.commandsPath=.\app\cmd -Daspectran.logsDir=%BASE_DIR%\app\logs -Daspectow.node.id=dev-console-node1 -Daspectow.node.console=true -Dtow.server.listener.http.port=8082 -Dtow.context.root.session.cookieName=JSESSIONID-8082 -Dtow.context.console.session.cookieName=JSESSIONID-8082 -Daspectow.console.config.db.h2.path_explicit=~/aspectow-console-demo"
