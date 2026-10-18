#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/env.sh
bash build.sh
port="${1:-8081}"
echo "Open http://localhost:$port after Spring reports 'Started'. Keep this terminal open."
echo "If this port is busy: bash run.sh 8082"
exec java -jar backend/target/oneroute-api.jar "--server.port=$port"
