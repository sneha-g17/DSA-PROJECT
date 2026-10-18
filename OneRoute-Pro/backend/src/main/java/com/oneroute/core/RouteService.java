package com.oneroute.core;

import com.oneroute.api.RouteRequest;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class RouteService {
    private static final List<String> OBJECTIVES = List.of("fastest", "cheapest", "fewest_transfers", "balanced");
    private static final List<String> MODES = List.of("all", "metro", "rail");
    private final Network network;
    private final NativeWorkerPool pool;
    private final Map<String, String> cache = new LinkedHashMap<>(256, 0.75f, true);

    public RouteService(@Value("${oneroute.root:.}") String directory) throws IOException {
        Path root = Path.of(directory).toAbsolutePath().normalize();
        network = new Network(root.resolve("data/network.json"));
        pool = new NativeWorkerPool(root, network);
    }

    public Map<String, Object> network() {
        return network.data;
    }

    public Map<String, Object> plan(RouteRequest query) throws IOException, InterruptedException {
        long start = System.nanoTime();
        validateIds(query);
        String key = network.revision + Json.stringify(query);
        synchronized (cache) {
            String snapshot = cache.get(key);
            if (snapshot != null) {
                Map<String, Object> result = Json.object(Json.parse(snapshot));
                result.put("cache_hit", true);
                result.put("request_ms", elapsed(start));
                return result;
            }
        }
        Map<String, Object> result = pool.execute(worker -> calculate(worker, query));
        synchronized (cache) {
            cache.put(key, Json.stringify(result));
            if (cache.size() > 256) {
                cache.remove(cache.keySet().iterator().next());
            }
        }
        result.put("request_ms", elapsed(start));
        return result;
    }

    private static double elapsed(long start) {
        return (System.nanoTime() - start) / 1e6;
    }

    private void validateIds(RouteRequest query) {
        Network.integer(query.source(), 0, network.stations.size() - 1);
        Network.integer(query.destination(), 0, network.stations.size() - 1);
        for (int id : query.closedStations()) Network.integer(id, 0, network.stations.size() - 1);
        for (int id : query.closedEdges()) Network.integer(id, 0, network.edges.size() - 1);
        for (int id : query.closedServices()) Network.integer(id, 1, network.services.size());
    }

    private Map<String, Object> calculate(NativeWorker worker, RouteRequest query) throws IOException, InterruptedException {
        List<Object> routes = new ArrayList<>();
        List<String> objectives = new ArrayList<>(List.of(query.objective()));
        if (query.compare()) {
            OBJECTIVES.stream().filter(value -> !value.equals(query.objective())).forEach(objectives::add);
        }
        double totalMicros = 0;
        for (String objective : objectives) {
            int count = objective.equals(query.objective()) ? query.alternatives() : 1;
            Map<String, Object> nativeResult = worker.request(protocol(query, objective, count));
            totalMicros += ((Number) nativeResult.get("engine_us")).doubleValue();
            List<Object> candidates = candidates(nativeResult, count);
            for (int index = 0; index < candidates.size(); index++) {
                merge(routes, Json.object(candidates.get(index)), objective, index + 1);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("routes", routes);
        response.put("query", query);
        response.put("revision", network.revision);
        response.put("data_mode", "demo");
        response.put("searches", objectives.size());
        response.put("total_engine_us", totalMicros);
        response.put("cache_hit", false);
        response.put("algorithm", "service-aware Dijkstra + Yen");
        return response;
    }

    private static List<Object> candidates(Map<String, Object> result, int count) {
        if (count > 1) return Json.array(result.get("routes"));
        if (Boolean.TRUE.equals(result.get("found"))) return List.of(result);
        return List.of();
    }

    private void merge(List<Object> routes, Map<String, Object> route, String objective, int rank) {
        for (Object item : routes) {
            Map<String, Object> existing = Json.object(item);
            if (existing.get("edges").equals(route.get("edges"))) {
                if (rank == 1) Json.array(existing.get("optimal_for")).add(objective);
                return;
            }
        }
        route.put("optimal_for", new ArrayList<>(rank == 1 ? List.of(objective) : List.of()));
        route.put("rank", rank);
        route.put("rank_objective", objective);
        enrich(route);
        routes.add(route);
    }

    private void enrich(Map<String, Object> route) {
        List<Object> legs = new ArrayList<>();
        List<Object> edges = Json.array(route.get("edges"));
        List<Object> stations = Json.array(route.get("stations"));
        List<Object> waits = Json.array(route.get("waits"));
        long distance = 0;
        for (int i = 0; i < edges.size(); i++) {
            int id = Network.integer(edges.get(i), 0, network.edges.size() - 1);
            Map<String, Object> leg = new LinkedHashMap<>(Json.object(network.edges.get(id)));
            leg.put("from", stations.get(i));
            leg.put("to", stations.get(i + 1));
            leg.put("wait_seconds", waits.get(i));
            distance += ((Number) leg.get("distance_m")).longValue();
            legs.add(leg);
        }
        route.put("legs", legs);
        route.put("distance_m", distance);
    }

    private static String protocol(RouteRequest query, String objective, int count) {
        StringBuilder line = new StringBuilder().append(query.source()).append(' ').append(query.destination())
                .append(' ').append(OBJECTIVES.indexOf(objective)).append(' ').append(MODES.indexOf(query.mode()));
        for (List<Integer> ids : List.of(query.closedStations(), query.closedEdges(), query.closedServices())) {
            line.append(' ').append(ids.size());
            ids.forEach(id -> line.append(' ').append(id));
        }
        return line.append(' ').append(count).toString();
    }

    @PreDestroy
    public void shutdown() {
        pool.close();
    }
}
