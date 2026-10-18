package com.oneroute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private JsonNode route(String body) throws Exception {
        String result = mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(result);
    }

    @Test void healthNetworkAndDocs() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/network")).andExpect(status().isOk()).andExpect(jsonPath("$.stations.length()").value(135));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/routes']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test void alternativesAccountingAndCache() throws Exception {
        String body = "{\"source\":0,\"destination\":25,\"compare\":false,\"alternatives\":3}";
        JsonNode result = route(body);
        assertTrue(result.get("routes").size() > 0);
        assertTrue(result.get("routes").size() <= 3);
        long previous = -1;
        java.util.Set<String> paths = new java.util.HashSet<>();
        for (JsonNode candidate : result.get("routes")) {
            assertTrue(paths.add(candidate.get("edges").toString()));
            long seconds = candidate.get("seconds").asLong();
            assertTrue(seconds >= previous); previous = seconds;
            long total = 0, distance = 0;
            for (JsonNode leg : candidate.get("legs")) {
                total += leg.get("seconds").asLong() + leg.get("wait_seconds").asLong();
                distance += leg.get("distance_m").asLong();
            }
            assertEquals(total, seconds);
            assertEquals(distance, candidate.get("distance_m").asLong());
        }
        assertTrue(route(body).get("cache_hit").asBoolean());
    }

    @Test void closuresAndSameStation() throws Exception {
        JsonNode same = route("{\"source\":0,\"destination\":0,\"compare\":false}");
        assertEquals(0, same.get("routes").get(0).get("seconds").asInt());
        JsonNode closed = route("{\"source\":0,\"destination\":25,\"closed_stations\":[0]}");
        assertEquals(0, closed.get("routes").size());
    }

    @Test void rejectsInvalidQueries() throws Exception {
        String[] invalid = {
            "{}", "{\"source\":-1,\"destination\":0}", "{\"source\":999,\"destination\":0}",
            "{\"source\":0.5,\"destination\":0}", "{\"source\":true,\"destination\":0}",
            "{\"source\":\"0\",\"destination\":0}", "{\"source\":0,\"destination\":0,\"alternatives\":4}",
            "{\"source\":0,\"destination\":0,\"objective\":\"unknown\"}",
            "{\"source\":0,\"destination\":0,\"extra\":1}",
            "{\"source\":0,\"source\":1,\"destination\":0}",
            "{\"source\":0,\"destination\":0,\"closed_services\":[99]}"
        };
        for (String body : invalid) mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test void localBoundaryAndPayloadLimit() throws Exception {
        mvc.perform(get("/api/health").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(" ".repeat(16385)))
                .andExpect(status().isPayloadTooLarge());
    }
}
