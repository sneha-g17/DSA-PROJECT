# OneRoute Pro — Chennai transit planning
C++17 routing engine · Java Spring Boot API · React interface

A portfolio application with service-aware shortest paths, genuine Yen alternatives, closure simulation and inspectable API documentation. No Python is used or included.

**Demo data only:** 135 graph nodes, 146 undirected links and six services. Times, waits, fares, distances and transfer connections are estimates. The schematic is not a geographic map. Coverage is not a claim about currently operating services. Do not use this application to make real travel decisions.

## Start here — Windows (your existing setup)

Extract this ZIP into a NEW folder. Do not merge it into the previous edition. Open the extracted **oneroute-pro** folder in VS Code; it contains this README and run.sh.

You already installed the required C++ compiler and JDK21. Maven is downloaded automatically on the first build, which needs internet access. Node.js is NOT required to run the included prebuilt React interface.

### Option A: MSYS2 UCRT64 terminal

Use the directory where you actually extracted the ZIP. For example:

```bash
cd /c/Users/sneha/Downloads/oneroute-pro
bash run.sh 8081
```

If extraction created an additional outer folder, enter the inner folder first. Run `ls run.sh` to confirm. After Spring reports **Started**, open http://localhost:8081. Leave the terminal running; Ctrl+C stops it.

### Option B: VS Code PowerShell terminal

```powershell
cd "C:\Users\sneha\Downloads\oneroute-pro"
.\run.bat 8081
```

If port8081 is already occupied by your old app, use `bash run.sh 8082` or `.\run.bat 8082` and open http://localhost:8082. Do not terminate an unknown process.

## Requirements

- C++17 compiler: your MSYS2 UCRT64 GCC is suitable; old MinGW GCC6.3 is not.
- JDK17–25; JDK21 is recommended for your installed environment.
- Internet for the first Maven build. The wrapper downloads Maven3.9.16 and pinned backend dependencies.
- Node22+ and npm only when editing or testing React.
- Scripts set environment paths only within their own process. They do not change system variables.

On Linux, install a C++17 compiler, JDK and curl or wget plus unzip, then run `bash run.sh`.

## Included upgrades

| Module | Implementation |
|---|---|
| C++ routing | Sparse adjacency lists, priority-queue Dijkstra, service/walking state |
| Alternatives | Yen root/spur algorithm, up to three station-simple paths for the selected objective |
| Java API | Spring Boot controllers, DTO validation, service layer, exception advice |
| API documentation | Swagger UI at /swagger-ui.html; OpenAPI JSON at /v3/api-docs |
| Native bridge | Two persistent C++ workers, bounded leases, reply timeout, restart-on-failure |
| Performance | Revision/constraint-aware LRU cache with 256 entries |
| React UI | Route comparison, alternatives selector, schematic highlight, filters and estimates |
| Demo features | Service closures, local-browser favourites, JSON export |
| Verification | C++ behavioural tests, 600 exhaustive-oracle comparisons, Spring API tests, React/live-backend test |

The selected objective can be fastest, cheapest, fewest transfers or a documented balanced score. Comparing objectives can produce additional cards beyond the requested three alternatives; duplicates are merged. A route is tagged “optimal” only when it is first for that objective.

## Useful pages

- Application: http://localhost:8081
- Interactive API docs: http://localhost:8081/swagger-ui.html
- OpenAPI specification: http://localhost:8081/v3/api-docs
- Health: http://localhost:8081/api/health

## Tests and frontend development

```bash
bash check.sh
cd frontend
npm ci
npm run build
cd ..
bash build.sh
cd frontend
npm test
```

For PowerShell use `.\check.bat` and `.\build.bat`. Rebuild the backend after changing React because the frontend is packaged into the Spring Boot JAR. The UI integration test starts and stops its own server on a free port.

## Troubleshooting

- **java not found:** in UCRT64 run `export JAVA_HOME="/c/Program Files/Java/jdk-21"`, then rerun. Launch scripts autodetect this exact common installation when JAVA_HOME is absent.
- **Wrong JAVA_HOME:** it must point to the JDK folder, not its bin folder.
- **g++ silent failure / old MinGW:** use UCRT64. PowerShell scripts prioritize C:\msys64\ucrt64\bin when present.
- **Dependency download fails:** check internet/proxy settings; rerun the build. No dependency errors are suppressed.
- **Address already in use:** select another port, e.g.8082.
- **Native engine missing:** start with run.sh/run.bat from the project root, not the Java file’s Run button.
- **Sonar warnings:** this edition replaces the old Server.java with separate Spring components. Automated tests passed, but a SonarQube quality-gate scan has not been run.
- **Swagger fails after changing ports:** use the same port as the app.

## Project layout

- src/, include/: C++ engine and line-protocol executable
- tests/: native tests and exhaustive Yen oracle
- backend/: Maven/Spring Boot source, API tests and Maven wrapper
- frontend/: editable React source
- web/: prebuilt React assets, included for easy startup
- data/network.json: versioned, validated demo dataset
- docs/: architecture, API contract, data inventory and interview guide

Read docs/ARCHITECTURE.md and docs/INTERVIEW.md before presenting the project. There is no MySQL, JWT, live railway integration or persistent admin console in this edition; those are future modules, not claimed features.

## Optional Docker

```bash
docker build -t oneroute-pro .
docker run --rm -p 127.0.0.1:8081:8081 oneroute-pro
```

The image definition is included but was not executed in this delivery environment. This is a local demo, not a hardened public deployment.
