package com.homesvc.booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesvc.dto.BookingEventDTO;
import com.homesvc.booking.model.NotificationType;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class NotificationDispatcher {

    private static final String BASE = System.getProperty("notification.service.base-url", "http://localhost:8084");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public void dispatch(BookingEventDTO dto) throws Exception {
        boolean success = "SUCCESS".equalsIgnoreCase(dto.getStatus());
        NotificationType type = success ? NotificationType.BOOKING_SUCCESS : NotificationType.BOOKING_FAILURE;
        String msgCustomer = success
                ? "Your booking #" + dto.getBookingId() + " was confirmed."
                : "Your booking failed: " + (dto.getMessage() != null ? dto.getMessage() : "Unknown error");
        String msgProvider = success
                ? "Booking #" + dto.getBookingId() + " was confirmed for your offer."
                : "A booking attempt for your offer failed: " + (dto.getMessage() != null ? dto.getMessage() : "Unknown error");

        postNotification(dto.getCustomerId(), msgCustomer, type);
        if (dto.getProviderId() != null && dto.getProviderId() > 0) {
            postNotification(dto.getProviderId(), msgProvider, type);
        }
    }

    private void postNotification(Long userId, String message, NotificationType type) throws Exception {
        String json = mapper.writeValueAsString(Map.of(
                "userId", userId,
                "message", message,
                "type", type.name()));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/notifications"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("Notification POST failed: " + resp.statusCode() + " " + resp.body());
        }
    }
}
