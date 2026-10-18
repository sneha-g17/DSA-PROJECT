@echo off
setlocal
cd /d "%~dp0"
call scripts\env.bat
if errorlevel 1 exit /b 1
call build.bat
if errorlevel 1 exit /b 1
for %%T in (engine yen) do (
  g++ -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror -Iinclude src/engine.cpp tests/test_%%T.cpp -o build/test_%%T.exe
  if errorlevel 1 exit /b 1
  build\test_%%T.exe
  if errorlevel 1 exit /b 1
)
pushd backend
call mvnw.cmd -B test
set "ONEROUTE_TEST_RESULT=%ERRORLEVEL%"
popd
exit /b %ONEROUTE_TEST_RESULT%
