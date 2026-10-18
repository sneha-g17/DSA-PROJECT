# API

Run the app, then open /swagger-ui.html for interactive documentation. /v3/api-docs returns the generated OpenAPI specification. All requests use the same host and port as the UI.

## Endpoints

| Method | Path | Result |
|---|---|---|
| GET | /api/health | Local application status and dataset revision |
| GET | /api/network | Complete demo station, service and edge data |
| POST | /api/routes | Ranked alternatives and optional objective comparisons |
| GET | /actuator/health | Spring health information |

## Example request

```json
{
  "source": 0,
  "destination": 25,
  "objective": "fastest",
  "mode": "all",
  "compare": true,
  "alternatives": 3,
  "closed_stations": [],
  "closed_edges": [],
  "closed_services": []
}
```

IDs come from /api/network, not the 1-based numbers in the original 40-station program. Objectives: fastest, cheapest, fewest_transfers, balanced. Modes: all, metro, rail. Walking connectors remain permitted in each mode. Each closure list is limited to30 IDs. alternatives is1–3, default3. compare defaults true.

## Response semantics

routes is an array, empty when no route exists. Each route contains station/edge IDs, legs, seconds, ride_seconds, walk_seconds, wait_seconds, fare_paise, distance_m, transfers, rank, rank_objective and optimal_for.

- rank applies to rank_objective only.
- optimal_for lists objectives for which this exact route is the first shortest result.
- Yen ranks2 and3 are not mislabeled as globally optimal.
- With compare=true, distinct routes from three other objectives may be added, so the response can contain more than three cards.
- Same-source/destination returns a zero-leg route unless the station is closed.
- seconds = ride_seconds + walk_seconds + wait_seconds.
- fares are integer paise; the UI divides by100 for rupees.
- total_engine_us measures original native query work. On a cache hit it remains the stored computation cost, not fresh work.
- request_ms measures current service processing time, excluding network transit and JSON response serialization.
- searches counts objective-level calls, not Yen's internal Dijkstra spur searches.
- expanded/relaxed on reconstructed Yen candidates are not aggregate search counters.

The response includes data_mode=demo, revision, algorithm and cache_hit.

## Errors

400: malformed JSON, unknown fields, invalid IDs/types/objective/k/closures.
403: nonlocal host or disallowed Origin.
413: POST body larger than16KiB.
415: unsupported request media type.
503: worker unavailable, busy or interrupted.

Validation and worker errors use an error field; selected framework errors may use Spring's default error shape. No real-time data or ticket booking endpoint is provided.
