package com.homesvc.booking.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CategoryCacheBean {

    private static final String OFFER_BASE = System.getProperty("offer.service.base-url", "http://localhost:8083");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private volatile List<String> categoryNames = Collections.emptyList();

    @PostConstruct
    public void load() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OFFER_BASE + "/categories"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                return;
            }
            JsonNode arr = mapper.readTree(resp.body());
            List<String> names = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n.has("name")) {
                        names.add(n.get("name").asText());
                    }
                }
            }
            categoryNames = List.copyOf(names);
        } catch (Exception ignored) {
            categoryNames = Collections.emptyList();
        }
    }

    public List<String> getCategories() {
        return categoryNames;
    }
}
