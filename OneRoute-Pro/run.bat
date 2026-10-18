@echo off
setlocal
cd /d "%~dp0"
call scripts\env.bat
if errorlevel 1 exit /b 1
call build.bat
if errorlevel 1 exit /b 1
set "ONEROUTE_PORT=8081"
if not "%~1"=="" set "ONEROUTE_PORT=%~1"
echo Open http://localhost:%ONEROUTE_PORT% after Spring reports Started.
echo Keep this terminal open. If port is busy, run: .\run.bat 8082
java -jar backend/target/oneroute-api.jar --server.port=%ONEROUTE_PORT%
exit /b %ERRORLEVEL%
