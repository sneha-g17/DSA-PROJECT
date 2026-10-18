# Architecture and correctness

## Responsibilities

React sends a validated JSON request to Spring's RouteController. RouteService checks dataset IDs, looks up the cache, and leases a persistent native worker. C++ executes Dijkstra/Yen and returns JSON. Java adds leg details and distances; React renders the result.

| Component | Responsibility |
|---|---|
| RouteRequest | Typed input, defaults, closure normalization and Bean Validation |
| RouteController | HTTP mapping and OpenAPI documentation |
| ApiExceptionHandler | Consistent validation/unavailable responses and logging |
| RouteService | Objective comparison, cache keys, native protocol, route enrichment |
| Network | Strict dataset validation and graph export |
| NativeWorkerPool | Two workers, two-second lease timeout, lifecycle cleanup |
| NativeWorker | ProcessBuilder without a shell, five-second reply timeout, recovery |
| LocalRequestFilter | Local-host/origin boundary, 16KiB POST cap, response headers |
| Engine | All shortest-path and alternative-path computation |

The old handwritten HTTP server and JSON parser are removed. Spring owns HTTP lifecycle; Jackson owns JSON parsing. No claim is made that a SonarQube scan was executed.

## Why station-only Dijkstra is insufficient

At the same station, arriving on Blue versus Green changes whether the next edge incurs a boarding wait and transfer. The state is (station, last boarded service, walked since boarding). Walking preserves the remembered service; reboarding after a walk incurs a new wait even on the same service.

First boarding does not count as a transfer. Subsequent boarding after a service change or walking does. Waits are fixed service-wide estimates, not timetable calculations.

## Objectives and units

| Objective | Lexicographic cost tuple |
|---|---|
| Fastest | seconds, transfers, fare_paise |
| Cheapest | fare_paise, seconds, transfers |
| Fewest transfers | transfers, seconds, fare_paise |
| Balanced | 5 × seconds + fare_paise + 900 × transfers, seconds, fare_paise |

Balanced weights are an explicit demonstration preference, not a calibrated passenger utility model. Fare is an additive estimated edge cost, not an official zonal or integrated ticket calculation. Distance is displayed but is not an optimization objective.

## Yen alternatives

Start with the shortest path. For each spur point of the last accepted path:

1. Preserve the root prefix and its service/walking context.
2. Ban prior root stations, and block next edges of accepted paths sharing the root.
3. Run Dijkstra for the spur and recompute the combined path's full costs.
4. Store unseen candidates in a persistent min-heap.
5. Accept the best remaining candidate and repeat until k paths or exhaustion.

Paths are distinct by edge sequence; parallel services may produce distinct paths with the same station sequence. Returned alternatives are station-simple under this static model. At equal cost, any valid tied path may be selected. k is limited to 1–3.

The service-context carry is essential: resetting it at each spur would double-charge or omit waiting and transfers. The algorithm is tested against independent exhaustive enumeration, not against itself.

## Complexity

Let N be physical stations, S services, V'=2N(S+1) expanded states and E' the possible state transitions. Sparse storage avoids the original N×N adjacency matrix.

For this lazy binary-heap implementation, one Dijkstra search takes O((V'+E') log(V'+E')) time and O(V'+E') working memory in the worst case. For fixed service count and a sparse transport graph this is commonly written O((N+E) log N).

Yen makes at most 1+(k−1)(N−1) shortest-path calls, plus candidate construction, prefix checks and heap ordering. Its main search cost is O(kN(V'+E') log(V'+E')); it is deliberately more expensive than one shortest path. Comparing the other three objectives adds at most three Dijkstra calls. There is no claim of universally “best” complexity.

## Cache and operational limits

The cache key contains network revision, endpoints, objective, mode, all normalized closures, compare flag and k. JSON snapshots prevent response mutations from corrupting cached values. Concurrent identical misses may compute twice; there is no single-flight mechanism.

Two persistent workers amortize process startup. A busy pool returns503 after its lease timeout; a failed/timed-out worker is restarted and the request returns503 for the client to retry. Shutdown closes workers and deletes the temporary graph. There is no database, cross-process cache or distributed deployment.

## Limitations

Edges are undirected and operational metrics are static estimates. No departure-time model, live disruption feed, accessibility guarantee or current-network verification. The local boundary is not authentication. Public deployment would require security review, authorization where applicable, TLS, rate limits, observability and verified operational data.
