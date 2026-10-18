package com.oneroute.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Demo route query. IDs come from GET /api/network; all operational values are estimates.")
public record RouteRequest(
        @NotNull @Min(0) @Schema(example = "0") Integer source,
        @NotNull @Min(0) @Schema(example = "25") Integer destination,
        @Pattern(regexp = "fastest|cheapest|fewest_transfers|balanced")
        @Schema(defaultValue = "fastest") String objective,
        @Pattern(regexp = "all|metro|rail") @Schema(defaultValue = "all") String mode,
        @Schema(defaultValue = "true") Boolean compare,
        @Min(1) @Max(3) @Schema(defaultValue = "3", description = "Maximum Yen routes for the selected objective") Integer alternatives,
        @Size(max = 30) List<@NotNull @Min(0) Integer> closedStations,
        @Size(max = 30) List<@NotNull @Min(0) Integer> closedEdges,
        @Size(max = 30) List<@NotNull @Min(1) Integer> closedServices) {
    public RouteRequest {
        objective = objective == null ? "fastest" : objective;
        mode = mode == null ? "all" : mode;
        compare = compare == null ? Boolean.TRUE : compare;
        alternatives = alternatives == null ? 3 : alternatives;
        closedStations = normalize(closedStations);
        closedEdges = normalize(closedEdges);
        closedServices = normalize(closedServices);
    }

    private static List<Integer> normalize(List<Integer> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > 30 || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Closure lists need at most 30 non-null IDs");
        }
        return values.stream().distinct().sorted().toList();
    }
}
