@echo off
chcp 65001 >nul
echo ========================================
echo   智慧园区管理系统 - 服务端启动脚本
echo ========================================
echo.

:: 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java运行环境，请安装JDK 17+
    pause
    exit /b 1
)

:: 设置JVM参数
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError

:: 查找JAR文件
set JAR_FILE=ruoyi-admin.jar
if not exist "%JAR_FILE%" (
    echo [错误] 未找到 %JAR_FILE%，请确保JAR文件在当前目录
    pause
    exit /b 1
)

echo [信息] 启动应用: %JAR_FILE%
echo [信息] JVM参数: %JAVA_OPTS%
echo.

:: 启动应用
java %JAVA_OPTS% -jar %JAR_FILE% --server.port=8080

pause
