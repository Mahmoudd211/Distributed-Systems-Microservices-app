package com.homesvc.wallet.service;

import com.homesvc.wallet.model.TransactionType;
import com.homesvc.wallet.model.Wallet;
import com.homesvc.wallet.model.WalletTransaction;
import com.homesvc.wallet.repo.WalletRepository;
import com.homesvc.wallet.repo.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Map<String, Long> createWallet(Long customerId, BigDecimal initialBalance) {
        if (walletRepository.findByCustomerId(customerId).isPresent()) {
            Wallet w = walletRepository.findByCustomerId(customerId).orElseThrow();
            return Map.of("walletId", w.getId());
        }
        Wallet w = new Wallet();
        w.setCustomerId(customerId);
        w.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO);
        w = walletRepository.save(w);
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            WalletTransaction tx = new WalletTransaction();
            tx.setWalletId(w.getId());
            tx.setAmount(initialBalance);
            tx.setType(TransactionType.CREDIT);
            tx.setReferenceId("INITIAL");
            transactionRepository.save(tx);
        }
        return Map.of("walletId", w.getId());
    }

    public Map<String, Object> getWallet(Long customerId) {
        Wallet w = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return Map.of("customerId", w.getCustomerId(), "balance", w.getBalance());
    }

    @Transactional
    public Map<String, Object> addFunds(Long customerId, BigDecimal amount) {
        Wallet w = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(w.getId());
        tx.setAmount(amount);
        tx.setType(TransactionType.CREDIT);
        transactionRepository.save(tx);
        return Map.of("newBalance", w.getBalance());
    }

    @Transactional
    public Map<String, Object> deduct(Long customerId, BigDecimal amount, String bookingRef) {
        Wallet w = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (transactionRepository.existsByWalletIdAndReferenceIdAndType(w.getId(), bookingRef, TransactionType.DEBIT)) {
            return Map.of("success", true, "newBalance", w.getBalance());
        }
        if (w.getBalance().compareTo(amount) < 0) {
            return Map.of("success", false, "reason", "Insufficient balance");
        }
        w.setBalance(w.getBalance().subtract(amount));
        walletRepository.save(w);
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(w.getId());
        tx.setAmount(amount);
        tx.setType(TransactionType.DEBIT);
        tx.setReferenceId(bookingRef);
        transactionRepository.save(tx);
        return Map.of("success", true, "newBalance", w.getBalance());
    }

    @Transactional
    public Map<String, Object> refund(Long customerId, BigDecimal amount, String bookingRef) {
        Wallet w = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        if (transactionRepository.existsByWalletIdAndReferenceIdAndType(w.getId(), bookingRef, TransactionType.REFUND)) {
            return Map.of("success", true, "newBalance", w.getBalance());
        }
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(w.getId());
        tx.setAmount(amount);
        tx.setType(TransactionType.REFUND);
        tx.setReferenceId(bookingRef);
        transactionRepository.save(tx);
        return Map.of("success", true, "newBalance", w.getBalance());
    }
}
