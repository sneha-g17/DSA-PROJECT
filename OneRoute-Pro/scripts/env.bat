@echo off
rem Changes apply to the calling script's setlocal scope only.
if exist "C:\msys64\ucrt64\bin\g++.exe" set "PATH=C:\msys64\ucrt64\bin;%PATH%"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Install JDK 17-25 and set JAVA_HOME to its folder.
  exit /b 1
)
where g++ >nul 2>&1
if errorlevel 1 (
  echo ERROR: Install the MSYS2 UCRT64 GCC compiler.
  exit /b 1
)
exit /b 0
