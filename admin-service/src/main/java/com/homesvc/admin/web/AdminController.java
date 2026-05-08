package com.homesvc.admin.web;

import com.homesvc.admin.model.AdminLog;
import com.homesvc.admin.repo.AdminLogRepository;
import com.homesvc.admin.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RestTemplate restTemplate;
    private final AdminLogRepository adminLogRepository;
    private final JwtUtil jwtUtil;
    private final String authBase;
    private final String bookingBase;
    private final String offerBase;

    public AdminController(
            RestTemplate forwardingRestTemplate,
            AdminLogRepository adminLogRepository,
            JwtUtil jwtUtil,
            @Value("${auth.service.base-url}") String authBase,
            @Value("${booking.service.base-url}") String bookingBase,
            @Value("${offer.service.base-url}") String offerBase) {
        this.restTemplate = forwardingRestTemplate;
        this.adminLogRepository = adminLogRepository;
        this.jwtUtil = jwtUtil;
        this.authBase = authBase;
        this.bookingBase = bookingBase;
        this.offerBase = offerBase;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        try {
            HttpHeaders headers = authHeaders();
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    authBase + "/users",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    });
            logAction("LIST_USERS", null);
            return ResponseEntity.ok(resp.getBody());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(Map.of("error", "Auth service unavailable"));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> transactions() {
        try {
            HttpHeaders headers = authHeaders();
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    bookingBase + "/bookings",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    });
            logAction("LIST_BOOKINGS", null);
            return ResponseEntity.ok(resp.getBody());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(Map.of("error", "Booking service unavailable"));
        }
    }

    public record CreateCategoryBody(String name) {
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody CreateCategoryBody body) {
        try {
            HttpHeaders headers = authHeaders();
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    offerBase + "/categories",
                    new HttpEntity<>(body, headers),
                    Map.class);
            logAction("CREATE_CATEGORY", null);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(Map.of("error", "Offer service unavailable"));
        }
    }

    @GetMapping("/logs")
    public List<AdminLog> logs() {
        return adminLogRepository.findAllByOrderByPerformedAtDesc();
    }

    private HttpHeaders authHeaders() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) a.getCredentials();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private void logAction(String action, Long targetId) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) a.getCredentials();
        Long adminId = jwtUtil.extractUserId(token);
        AdminLog log = new AdminLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetId(targetId);
        adminLogRepository.save(log);
    }
}
