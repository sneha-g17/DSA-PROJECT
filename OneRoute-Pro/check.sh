#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/env.sh
bash build.sh
for name in engine yen; do
  "${CXX:-g++}" -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror -Iinclude src/engine.cpp "tests/test_$name.cpp" -o "build/test_$name"
  "./build/test_$name"
done
(cd backend && bash mvnw -B test)
echo "Native and Spring API tests passed."
