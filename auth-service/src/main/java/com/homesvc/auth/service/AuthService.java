package com.homesvc.auth.service;

import com.homesvc.auth.dto.LoginRequest;
import com.homesvc.auth.dto.RegisterCustomerRequest;
import com.homesvc.auth.dto.RegisterProviderRequest;
import com.homesvc.auth.dto.UserSummaryDto;
import com.homesvc.auth.model.Role;
import com.homesvc.auth.model.User;
import com.homesvc.auth.repo.UserRepository;
import com.homesvc.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final String walletBaseUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RestTemplate restTemplate,
            @Value("${wallet.service.base-url}") String walletBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
        this.walletBaseUrl = walletBaseUrl;
    }

    @Transactional
    public User registerCustomer(RegisterCustomerRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new UsernameConflictException();
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.CUSTOMER);
        u = userRepository.save(u);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("customerId", u.getId());
            body.put("initialBalance", req.getInitialBalance() != null ? req.getInitialBalance() : BigDecimal.ZERO);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(
                    walletBaseUrl + "/wallet/create",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Wallet create failed for customer {}: {}", u.getId(), e.getMessage());
        }
        return u;
    }

    @Transactional
    public User registerProvider(RegisterProviderRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new UsernameConflictException();
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.PROVIDER);
        u.setProfessionType(req.getProfessionType());
        return userRepository.save(u);
    }

    public Map<String, Object> login(LoginRequest req) {
        User u = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(u.getId(), u.getRole().name());
        return Map.of(
                "token", token,
                "userId", u.getId(),
                "role", u.getRole().name());
    }

    public List<UserSummaryDto> listAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getRole().name()))
                .collect(Collectors.toList());
    }

    public UserSummaryDto getUser(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserSummaryDto(u.getId(), u.getUsername(), u.getRole().name());
    }

    public Map<String, Object> validate(String bearerToken) {
        String token = extractBearer(bearerToken);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Map.of("valid", false);
        }
        return Map.of(
                "valid", true,
                "userId", jwtUtil.extractUserId(token),
                "role", jwtUtil.extractRole(token));
    }

    private static String extractBearer(String header) {
        if (header == null) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    public static class UsernameConflictException extends RuntimeException {
    }
}
