package com.oneroute.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfiguration {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info().title("Chennai OneRoute Pro API").version("5.0.0")
                .description("C++ Dijkstra + Yen, Spring Boot and React. Demonstration data only; no live railway feed or real ticket prices."));
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictNumbers() {
        return builder -> builder.featuresToDisable(
                MapperFeature.ALLOW_COERCION_OF_SCALARS, DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
