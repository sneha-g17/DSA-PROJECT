# Interview walkthrough

## A two-minute demonstration

1. Open the application and show its clear demo-data label.
2. Pick an example journey; explain duration as ride + walk + waiting.
3. Request three alternatives; distinguish Yen ranking from cheapest/fastest objective comparison.
4. Close a service and rerun. Explain that closures change the graph used for this query.
5. Repeat a query to show the cache hit.
6. Open Swagger and execute POST /api/routes with invalid input to show validation.
7. Show tests/test_yen.cpp and explain its independent exhaustive oracle.

## Questions to prepare

**Why C++ and Java?** C++ contains the routing algorithm; Java handles HTTP, validation, worker lifecycle and caching. This separation adds an IPC contract and deployment complexity. For this small network one language could also work; the split is an architectural learning choice, not a proven performance necessity.

**Why not an adjacency matrix?** The graph is sparse. Adjacency lists visit connected edges rather than scanning every station for each expansion.

**Why more than one state per station?** Transfer/wait cost depends on the previous service and whether the traveller walked before reboarding.

**Are three objectives the same as three shortest paths?** No. Objective comparison solves different optimization problems. Yen ranks distinct paths under one objective.

**How do you know Yen is correct?** An independent DFS enumerates all station-simple paths on150 generated small graphs. The top-three cost tuples match across four objectives:600 comparisons. This is strong test evidence, not a formal proof for every possible network.

**How fast is it?** Quote measured results only from your own machine and record hardware, compiler, query set and cache state. Explain complexity from ARCHITECTURE.md; don't invent latency claims.

**Is this a real-time transit application?** No. It is a working full-stack route-planning prototype with estimated static data. Verified fares, schedules and disruption feeds are future work.

**Is it production-ready?** No. Local safeguards and tests are included, but there is no public authentication, deployment security review, live data SLA or production load study.

## Suggested next modules

Timetable-aware routing, verified fare rules, geographic mapping, authenticated saved journeys and a controlled data-editing workflow. Add one at a time with tests rather than claiming unimplemented features.

Use your own words. Be ready to trace one query through React, the controller, service, native worker and C++ state graph.
