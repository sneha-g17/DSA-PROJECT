@echo off
setlocal
cd /d "%~dp0"
call scripts\env.bat
if errorlevel 1 exit /b 1
if not exist build mkdir build
echo [1/2] Compiling C++17 routing engine...
g++ -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror -Iinclude src/main.cpp src/engine.cpp -o build/transit_engine.exe
if errorlevel 1 (
  echo ERROR: C++ compilation failed. Try bash run.sh in the MSYS2 UCRT64 terminal.
  exit /b 1
)
echo [2/2] Packaging Spring Boot API. First build downloads dependencies.
pushd backend
call mvnw.cmd -B -DskipTests package
set "ONEROUTE_BUILD_RESULT=%ERRORLEVEL%"
popd
if not "%ONEROUTE_BUILD_RESULT%"=="0" exit /b %ONEROUTE_BUILD_RESULT%
echo Build complete.
exit /b 0
