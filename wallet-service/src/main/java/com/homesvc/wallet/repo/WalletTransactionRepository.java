package com.homesvc.wallet.repo;

import com.homesvc.wallet.model.TransactionType;
import com.homesvc.wallet.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    boolean existsByWalletIdAndReferenceIdAndType(Long walletId, String referenceId, TransactionType type);
}
