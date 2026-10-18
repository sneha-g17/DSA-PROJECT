#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/env.sh
trap 'echo "Build failed. Read the error above; no server was started." >&2' ERR
mkdir -p build
suffix=""
case "$(uname -s)" in MINGW*|MSYS*) suffix=".exe";; esac
echo "[1/2] Compiling C++17 routing engine..."
"${CXX:-g++}" -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror -Iinclude src/main.cpp src/engine.cpp -o "build/transit_engine$suffix"
echo "[2/2] Packaging Spring Boot API (first build downloads Maven/dependencies)..."
(cd backend && bash mvnw -B -DskipTests package)
echo "Build complete."
