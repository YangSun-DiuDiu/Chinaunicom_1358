@echo off
chcp 65001 >nul
:: ============================================================
:: RuoYi-Vue SNMP工具安装脚本
:: 用途: 在Windows上安装Net-SNMP (含snmpwalk)
:: 日期: 2026-06-16
:: ============================================================
title RuoYi-Vue SNMP工具安装程序

echo.
echo +===========================================+
echo ^|   智慧园区管理系统 - SNMP工具安装程序     ^|
echo +===========================================+
echo.
echo 当前系统将安装 Net-SNMP 工具集（含 snmpwalk.exe）
echo 支持 SNMP v1/v2c/v3 协议
echo.

set "SNMP_DIR=C:\net-snmp"
set "SNMP_BIN=%SNMP_DIR%\bin"
set "INSTALLER_URL=https://sourceforge.net/projects/net-snmp/files/net-snmp%%20binaries/5.5-binaries/net-snmp-5.5.0-2.x64.exe/download"

:: 检查是否已安装
where snmpwalk >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [✓] 检测到 snmpwalk 已安装:
    snmpwalk --version 2>&1 | findstr /i "version"
    echo.
    echo 安装已完成，无需重复安装。
    echo.
    pause
    exit /b 0
)

:: 检查是否曾经解压到 C:\net-snmp
if exist "%SNMP_BIN%\snmpwalk.exe" (
    echo [✓] 检测到 %SNMP_BIN%\snmpwalk.exe 存在
    goto :check_path
)

echo [1/4] 正在下载 Net-SNMP 安装包...
echo 提示: 如果下载失败，请手动从以下地址下载:
echo   https://sourceforge.net/projects/net-snmp/files/net-snmp%%20binaries/5.5-binaries/
echo   下载 net-snmp-5.5.0-2.x64.exe 后放到与本脚本相同目录重试
echo.

:: 检查当前目录是否有安装包
set "LOCAL_INSTALLER="
for %%f in (net-snmp*.exe) do (
    set "LOCAL_INSTALLER=%%f"
    echo [✓] 发现本地安装包: %%f
    goto :install_local
)

:: 尝试下载
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri '%INSTALLER_URL%' -OutFile '%TEMP%\net-snmp-installer.exe' -UseBasicParsing -TimeoutSec 120 } catch { Write-Host 'Download failed: ' \$_.Exception.Message; exit 1 }"

if exist "%TEMP%\net-snmp-installer.exe" (
    set "LOCAL_INSTALLER=%TEMP%\net-snmp-installer.exe"
    goto :install_silent
)

echo.
echo [✗] 自动下载失败。请手动下载安装包后重试。
echo.
echo 手动安装步骤:
echo   1. 访问 https://sourceforge.net/projects/net-snmp/
echo   2. 下载 Windows 64-bit 版本
echo   3. 安装到 C:\net-snmp
echo   4. 将 C:\net-snmp\bin 添加到系统PATH
echo   5. 重新运行本脚本验证
echo.
pause
exit /b 1

:install_local
echo [2/4] 安装 Net-SNMP 到 %SNMP_DIR%...
if not exist "%SNMP_DIR%" mkdir "%SNMP_DIR%"
:: 尝试静默安装
"%LOCAL_INSTALLER%" /S /D=%SNMP_DIR% 2>nul
if exist "%SNMP_BIN%\snmpwalk.exe" goto :check_path

:: 如果静默安装失败，尝试手动解压 (Net-SNMP安装包实际是7z自解压)
echo 正在解压安装包...
if not exist "%SNMP_DIR%" mkdir "%SNMP_DIR%"
"%LOCAL_INSTALLER%" /extract:"%SNMP_DIR%" 2>nul
if exist "%SNMP_BIN%\snmpwalk.exe" goto :check_path

:: 尝试7z解压
where 7z >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    7z x "%LOCAL_INSTALLER%" -o"%SNMP_DIR%" -y >nul 2>&1
    if exist "%SNMP_BIN%\snmpwalk.exe" goto :check_path
)

echo [✗] 自动安装失败，请手动安装 Net-SNMP
pause
exit /b 1

:install_silent
echo [2/4] 静默安装 Net-SNMP 到 %SNMP_DIR%...
"%TEMP%\net-snmp-installer.exe" /S /D=%SNMP_DIR% 2>nul
if exist "%SNMP_BIN%\snmpwalk.exe" goto :check_path
echo [✗] 静默安装失败，请手动安装
pause
exit /b 1

:check_path
echo [3/4] 检查系统PATH配置...
echo "%PATH%" | findstr /i /c:"%SNMP_BIN%" >nul
if %ERRORLEVEL% EQU 0 (
    echo [✓] %SNMP_BIN% 已在PATH中
    goto :verify
)

echo 是否将 %SNMP_BIN% 添加到系统PATH? [Y/N]
set /p ADD_PATH="请输入 (Y/N): "
if /i "%ADD_PATH%"=="Y" (
    :: 添加到用户PATH
    for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH 2^>nul') do set "OLD_PATH=%%b"
    reg add "HKCU\Environment" /v PATH /t REG_EXPAND_SZ /d "%OLD_PATH%;%SNMP_BIN%" /f >nul 2>&1
    echo [✓] 已添加到用户PATH，重启终端后生效
    :: 立即生效（当前会话）
    set "PATH=%PATH%;%SNMP_BIN%"
) else (
    echo [i] 跳过PATH配置。需要在 application.yml 中配置完整路径:
    echo    snmp.walk-path: %SNMP_BIN:\=/%/snmpwalk.exe
)

:verify
echo [4/4] 验证安装...
"%SNMP_BIN%\snmpwalk.exe" --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [✓] snmpwalk 安装成功!
    "%SNMP_BIN%\snmpwalk.exe" --version 2>&1 | findstr /i "version"
    echo.
    echo 安装位置: %SNMP_BIN%\snmpwalk.exe
    echo.
    echo 配置说明:
    echo   项目 application.yml 中已配置 snmp.walk-path: snmpwalk
    echo   如果 snmpwalk 在 PATH 中，无需额外配置即可自动识别
    echo   否则请在 application.yml 中配置完整路径:
    echo     snmp.walk-path: %SNMP_BIN:\=/%/snmpwalk.exe
    echo.
) else (
    echo [✗] 安装验证失败
    echo 请检查 %SNMP_BIN%\snmpwalk.exe 是否存在
)

echo.
echo +===========================================+
echo ^|      SNMP工具安装完成，请重启项目服务     ^|
echo +===========================================+
echo.
pause
exit /b 0
