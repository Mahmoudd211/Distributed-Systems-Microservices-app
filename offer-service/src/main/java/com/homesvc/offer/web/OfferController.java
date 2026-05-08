package com.homesvc.offer.web;

import com.homesvc.offer.model.Category;
import com.homesvc.offer.model.ServiceOffer;
import com.homesvc.offer.repo.CategoryRepository;
import com.homesvc.offer.repo.ServiceOfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class OfferController {

    private final CategoryRepository categoryRepository;
    private final ServiceOfferRepository offerRepository;

    public OfferController(CategoryRepository categoryRepository, ServiceOfferRepository offerRepository) {
        this.categoryRepository = categoryRepository;
        this.offerRepository = offerRepository;
    }

    public record CreateCategoryRequest(String name) {
    }

    public record CreateOfferRequest(String categoryName, BigDecimal price, LocalDate availableDate, String description) {
    }

    public record UpdateOfferRequest(BigDecimal price, LocalDate availableDate) {
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody CreateCategoryRequest req) {
        if (categoryRepository.findByName(req.name()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Category c = new Category();
        c.setName(req.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(c));
    }

    @GetMapping("/categories")
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/offers")
    public ResponseEntity<Map<String, Object>> createOffer(@RequestBody CreateOfferRequest req) {
        Long providerId = currentUserId();
        Category cat = categoryRepository.findByName(req.categoryName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown category"));
        ServiceOffer o = new ServiceOffer();
        o.setProviderId(providerId);
        o.setCategory(cat);
        o.setPrice(req.price());
        o.setAvailableDate(req.availableDate());
        o.setDescription(req.description());
        o.setActive(true);
        o = offerRepository.save(o);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", o.getId(), "message", "Offer created"));
    }

    @PutMapping("/offers/{id}")
    public ResponseEntity<Map<String, Object>> updateOffer(@PathVariable Long id, @RequestBody UpdateOfferRequest req) {
        Long providerId = currentUserId();
        ServiceOffer o = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!o.getProviderId().equals(providerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (req.price() != null) {
            o.setPrice(req.price());
        }
        if (req.availableDate() != null) {
            o.setAvailableDate(req.availableDate());
        }
        offerRepository.save(o);
        return ResponseEntity.ok(Map.of("message", "Updated"));
    }

    @GetMapping("/offers/category/{name}")
    public List<Map<String, Object>> offersByCategory(@PathVariable String name) {
        return offerRepository.findActiveByCategoryName(name, LocalDate.now()).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/offers/{id}")
    public ResponseEntity<Map<String, Object>> getOffer(@PathVariable Long id) {
        return offerRepository.findById(id)
                .map(o -> ResponseEntity.ok(toMap(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/offers/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        ServiceOffer o = offerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        o.setActive(false);
        offerRepository.save(o);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/offers/provider/{providerId}")
    public ResponseEntity<List<Map<String, Object>>> providerOffers(@PathVariable Long providerId) {
        Long jwtUser = currentUserId();
        if (!jwtUser.equals(providerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Map<String, Object>> list = offerRepository.findByProviderIdOrderByIdDesc(providerId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private Map<String, Object> toMap(ServiceOffer o) {
        return Map.of(
                "id", o.getId(),
                "providerId", o.getProviderId(),
                "categoryId", o.getCategory().getId(),
                "categoryName", o.getCategory().getName(),
                "price", o.getPrice(),
                "availableDate", o.getAvailableDate().toString(),
                "description", o.getDescription() != null ? o.getDescription() : "",
                "isActive", o.getActive() != null && o.getActive());
    }

    private static Long currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (Long) a.getPrincipal();
    }
}
