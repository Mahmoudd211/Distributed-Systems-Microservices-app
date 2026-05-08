package com.homesvc.auth.web;

import com.homesvc.auth.dto.LoginRequest;
import com.homesvc.auth.dto.RegisterCustomerRequest;
import com.homesvc.auth.dto.RegisterProviderRequest;
import com.homesvc.auth.dto.UserSummaryDto;
import com.homesvc.auth.model.User;
import com.homesvc.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<Map<String, Object>> registerCustomer(@RequestBody RegisterCustomerRequest req) {
        try {
            User u = authService.registerCustomer(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("userId", u.getId(), "message", "Customer registered"));
        } catch (AuthService.UsernameConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/register/provider")
    public ResponseEntity<Map<String, Object>> registerProvider(@RequestBody RegisterProviderRequest req) {
        try {
            User u = authService.registerProvider(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("userId", u.getId(), "message", "Provider registered"));
        } catch (AuthService.UsernameConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDto>> listUsers() {
        return ResponseEntity.ok(authService.listAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDto> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(authService.getUser(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(authService.validate(auth));
    }
}
