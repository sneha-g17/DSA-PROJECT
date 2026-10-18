package com.oneroute.api;

import com.oneroute.core.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Journey planning", description = "Local demo: fares, distances and times are estimates")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    @GetMapping("/health")
    @Operation(summary = "Application health and dataset revision")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "mode", "demo", "backend", "Spring Boot + C++",
                "revision", service.network().get("revision"));
    }

    @GetMapping("/network")
    @Operation(summary = "Stations, services, connections and demo assumptions")
    public Map<String, Object> network() {
        return service.network();
    }

    @PostMapping(value = "/routes", consumes = "application/json")
    @Operation(summary = "Rank up to three Yen alternatives and optionally compare objectives",
            description = "Ranking includes static boarding waits and transfers. Alternatives do not repeat stations. A tree-like part of the graph may have only one feasible route.")
    public Map<String, Object> routes(@Valid @RequestBody RouteRequest request) throws IOException, InterruptedException {
        return service.plan(request);
    }
}
