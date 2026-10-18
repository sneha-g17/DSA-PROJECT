#!/usr/bin/env bash
# Sourced by launch/build scripts. Changes this process only, not Windows settings.
case "$(uname -s)" in
  MINGW*|MSYS*)
    export PATH="/ucrt64/bin:$PATH"
    if [ -z "${JAVA_HOME:-}" ] && [ -x "/c/Program Files/Java/jdk-21/bin/java.exe" ]; then
      export JAVA_HOME="/c/Program Files/Java/jdk-21"
    fi
    ;;
esac
if [ -n "${JAVA_HOME:-}" ]; then
  if command -v cygpath >/dev/null 2>&1; then JAVA_HOME="$(cygpath -u "$JAVA_HOME")"; export JAVA_HOME; fi
  export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v java >/dev/null || { echo "JDK 17-25 required. Set JAVA_HOME to your JDK folder."; exit 1; }
command -v "${CXX:-g++}" >/dev/null || { echo "C++17 compiler missing. Use MSYS2 UCRT64 or install g++."; exit 1; }
