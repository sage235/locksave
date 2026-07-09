// module/transaction/repository/TransactionRepository.java

package com.LockSaveApplication.module.transaction.repository;

import com.LockSaveApplication.module.transaction.entity.Transaction;
import com.LockSaveApplication.module.transaction.enums.TransactionStatus;
import com.LockSaveApplication.module.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByVaultIdOrderByCreatedAtDesc(UUID vaultId, Pageable pageable);

    Optional<Transaction> findByTransactionReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // Total deposited into a vault (completed deposits only)
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.vault.id = :vaultId
            AND t.transactionType = 'DEPOSIT'
            AND t.status = 'COMPLETED'
            """)
    java.math.BigDecimal sumCompletedDeposits(@Param("vaultId") UUID vaultId);

    // All transactions across all vaults belonging to a user
    @Query("""
            SELECT t FROM Transaction t
            JOIN t.vault v
            WHERE v.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    Page<Transaction> findByVaultIdAndTransactionTypeOrderByCreatedAtDesc(
            UUID vaultId, TransactionType type, Pageable pageable);

    Page<Transaction> findByVaultIdAndStatusOrderByCreatedAtDesc(
            UUID vaultId, TransactionStatus status, Pageable pageable);
}