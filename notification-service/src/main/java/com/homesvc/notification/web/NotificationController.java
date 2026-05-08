package com.homesvc.notification.web;

import com.homesvc.notification.model.Notification;
import com.homesvc.notification.model.NotificationType;
import com.homesvc.notification.repo.NotificationRepository;
import com.homesvc.notification.security.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationRepository notificationRepository, JwtUtil jwtUtil) {
        this.notificationRepository = notificationRepository;
        this.jwtUtil = jwtUtil;
    }

    public record CreateNotificationRequest(Long userId, String message, NotificationType type) {
    }

    @PostMapping("/notifications")
    public ResponseEntity<Map<String, Long>> create(@RequestBody CreateNotificationRequest req) {
        Notification n = new Notification();
        n.setUserId(req.userId());
        n.setMessage(req.message());
        n.setType(req.type());
        n.setIsRead(false);
        n = notificationRepository.save(n);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", n.getId()));
    }

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<Notification>> list(@PathVariable Long userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        if (!authorizedForUser(userId, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        if (!authorizedForUser(n.getUserId(), auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications/{userId}/unread")
    public ResponseEntity<List<Notification>> unread(@PathVariable Long userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        if (!authorizedForUser(userId, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId));
    }

    private boolean authorizedForUser(Long userId, String authHeader) {
        String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader;
        if (token == null || !jwtUtil.validateToken(token)) {
            return false;
        }
        return jwtUtil.extractUserId(token).equals(userId);
    }
}
