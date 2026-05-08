package com.homesvc.wallet.web;

import com.homesvc.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    public record CreateWalletRequest(@NotNull Long customerId, BigDecimal initialBalance) {
    }

    public record AddFundsRequest(@NotNull Long customerId, @NotNull @Positive BigDecimal amount) {
    }

    public record DeductRequest(@NotNull Long customerId, @NotNull @Positive BigDecimal amount, @NotBlank String bookingRef) {
    }

    public record RefundRequest(@NotNull Long customerId, @NotNull @Positive BigDecimal amount, @NotBlank String bookingRef) {
    }

    @PostMapping("/wallet/create")
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateWalletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(req.customerId(), req.initialBalance()));
    }

    @GetMapping("/wallet/{customerId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long customerId) {
        try {
            return ResponseEntity.ok(walletService.getWallet(customerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/wallet/addFunds")
    public ResponseEntity<Map<String, Object>> addFunds(@Valid @RequestBody AddFundsRequest req) {
        try {
            return ResponseEntity.ok(walletService.addFunds(req.customerId(), req.amount()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/wallet/deduct")
    public ResponseEntity<Map<String, Object>> deduct(@Valid @RequestBody DeductRequest req) {
        try {
            Map<String, Object> result = walletService.deduct(req.customerId(), req.amount(), req.bookingRef());
            if (Boolean.FALSE.equals(result.get("success"))) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/wallet/refund")
    public ResponseEntity<Map<String, Object>> refund(@Valid @RequestBody RefundRequest req) {
        try {
            return ResponseEntity.ok(walletService.refund(req.customerId(), req.amount(), req.bookingRef()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
