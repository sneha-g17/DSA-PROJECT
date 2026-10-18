# Validation report

Verified on 2026-09-15 in the Linux delivery environment:

| Check | Result |
|---|---|
| Maven wrapper build (Maven3.9.16, JDK17) | Passed |
| C++17 build with warnings treated as errors | Passed |
| Native behavioural checks | 17 passed |
| Yen exhaustive-oracle comparisons | 600 passed across150 generated graphs |
| Spring Boot integration test methods | 5 passed, zero failures/errors |
| React with live Spring/C++ backend | Passed, no jsdom console errors |
| Bash script syntax checks | Passed |

The source includes:

- C++ behavioural tests covering route objectives, modes, closures, waiting, reboarding and accounting.
- An independent exhaustive path oracle:150 generated multimodal graphs, four objectives, top-three cost comparisons.
- Spring Boot/MockMvc integration tests using the real C++ binary, not a mock routing service.
- React/jsdom integration against a real HTTP Spring Boot/C++ process.

Run bash check.sh (or check.bat) and the frontend test instructions in README.md to reproduce.

No native Windows execution, Docker build, SonarQube quality-gate scan, browser visual inspection or production load certification was performed in this Linux delivery environment. Windows/MSYS2 scripts are supplied for your installed environment, but platform-specific behaviour must be checked on your computer.

No historical benchmark from a previous version is presented as a measurement of this edition.
