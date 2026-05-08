package com.homesvc.booking.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homesvc.dto.BookingEventDTO;
import com.homesvc.booking.entity.Booking;
import com.homesvc.booking.entity.BookingStatus;
import com.homesvc.booking.messaging.RabbitEventPublisher;
import com.homesvc.booking.messaging.RabbitTopology;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Stateless
public class BookingProcessorBean {

    private static final String OFFER_BASE = System.getProperty("offer.service.base-url", "http://localhost:8083");
    private static final String WALLET_BASE = System.getProperty("wallet.service.base-url", "http://localhost:8082");

    @PersistenceContext(unitName = "bookingPU")
    private EntityManager em;

    @EJB
    private RabbitEventPublisher rabbitEventPublisher;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Booking createBooking(Long customerId, Long offerId) throws IOException {
        OfferSnapshot offer = fetchOffer(offerId);
        if (offer == null) {
            return persistAndPublishFailure(customerId, null, offerId, null, null, "Offer not found");
        }
        if (!Boolean.TRUE.equals(offer.active)) {
            return persistAndPublishFailure(customerId, offer.providerId, offerId, offer.price, null, "Offer inactive");
        }

        String bookingRef = UUID.randomUUID().toString();
        WalletDeductResult deduct = walletDeduct(customerId, offer.price, bookingRef);
        if (!deduct.success()) {
            return persistAndPublishFailure(customerId, offer.providerId, offerId, offer.price, bookingRef, deduct.reason());
        }

        if (!deactivateOffer(offerId)) {
            walletRefund(customerId, offer.price, bookingRef);
            return persistAndPublishFailure(customerId, offer.providerId, offerId, offer.price, bookingRef, "Failed to deactivate offer");
        }

        Booking b = new Booking();
        b.setCustomerId(customerId);
        b.setProviderId(offer.providerId);
        b.setOfferId(offerId);
        b.setAmount(offer.price);
        b.setStatus(BookingStatus.CONFIRMED);
        em.persist(b);
        em.flush();

        publishEvent(b.getId(), customerId, offer.providerId, offer.price, "SUCCESS", "Booking confirmed",
                RabbitTopology.ROUTING_SUCCESS);
        return b;
    }

    private OfferSnapshot fetchOffer(Long offerId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OFFER_BASE + "/offers/" + offerId))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) {
                return null;
            }
            if (resp.statusCode() / 100 != 2) {
                return null;
            }
            JsonNode n = mapper.readTree(resp.body());
            Long providerId = n.get("providerId").asLong();
            BigDecimal price = new BigDecimal(n.get("price").toString());
            boolean active = n.has("isActive") && n.get("isActive").asBoolean();
            return new OfferSnapshot(providerId, price, active);
        } catch (Exception e) {
            return null;
        }
    }

    private WalletDeductResult walletDeduct(Long customerId, BigDecimal amount, String bookingRef) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "customerId", customerId,
                    "amount", amount,
                    "bookingRef", bookingRef));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WALLET_BASE + "/wallet/deduct"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode n = mapper.readTree(resp.body());
            if (resp.statusCode() == 400) {
                String reason = n.has("reason") ? n.get("reason").asText() : "Wallet error";
                return new WalletDeductResult(false, reason);
            }
            if (resp.statusCode() / 100 != 2) {
                return new WalletDeductResult(false, "Wallet service error");
            }
            if (n.has("success") && !n.get("success").asBoolean()) {
                String reason = n.has("reason") ? n.get("reason").asText() : "Wallet error";
                return new WalletDeductResult(false, reason);
            }
            return new WalletDeductResult(true, null);
        } catch (Exception e) {
            return new WalletDeductResult(false, e.getMessage());
        }
    }

    private void walletRefund(Long customerId, BigDecimal amount, String bookingRef) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "customerId", customerId,
                    "amount", amount,
                    "bookingRef", bookingRef));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WALLET_BASE + "/wallet/refund"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {
        }
    }

    private boolean deactivateOffer(Long offerId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OFFER_BASE + "/offers/" + offerId + "/deactivate"))
                    .timeout(Duration.ofSeconds(15))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

    private Booking persistAndPublishFailure(
            Long customerId,
            Long providerId,
            Long offerId,
            BigDecimal amount,
            String bookingRef,
            String reason) throws IOException {
        Booking b = new Booking();
        b.setCustomerId(customerId);
        b.setProviderId(providerId != null ? providerId : 0L);
        b.setOfferId(offerId);
        b.setAmount(amount != null ? amount : BigDecimal.ZERO);
        b.setStatus(BookingStatus.REJECTED);
        b.setFailureReason(reason);
        em.persist(b);
        em.flush();
        Long pid = (providerId != null && providerId > 0) ? providerId : null;
        publishEvent(b.getId(), customerId, pid, amount != null ? amount : BigDecimal.ZERO, "FAILURE", reason,
                RabbitTopology.ROUTING_FAILURE);
        return b;
    }

    private void publishEvent(Long bookingId, Long customerId, Long providerId, BigDecimal amount,
                              String status, String message, String routingKey) throws IOException {
        BookingEventDTO dto = new BookingEventDTO();
        dto.setBookingId(bookingId);
        dto.setCustomerId(customerId);
        dto.setProviderId(providerId != null && providerId > 0 ? providerId : null);
        dto.setAmount(amount);
        dto.setStatus(status);
        dto.setMessage(message);
        dto.setTimestamp(LocalDateTime.now());
        byte[] body = mapper.writeValueAsBytes(dto);
        rabbitEventPublisher.publish(routingKey, body);
    }

    private record OfferSnapshot(Long providerId, BigDecimal price, Boolean active) {
    }

    private record WalletDeductResult(boolean success, String reason) {
    }
}
