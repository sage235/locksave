// module/transaction/repository/TransactionRepository.java

package com.LockSaveApplication.module.transaction.repository;

import com.LockSaveApplication.module.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    Page<Transaction> findByVaultIdOrderByCreatedAtDesc(UUID vaultId, Pageable pageable);

    Optional<Transaction> findByTransactionReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // ── All transactions for a user across all their vaults ───────────────────
    // Safe JPQL — no enum filter, no cast needed
    @Query("""
            SELECT t FROM Transaction t
            JOIN t.vault v
            WHERE v.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByUserId(
            @Param("userId") UUID userId,
            Pageable pageable);

    // ── Sum completed deposits into a vault ───────────────────────────────────
    // Native query — enums hardcoded as string literals with CAST
    // avoids Hibernate binding them as varchar at runtime
    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE vault_id = :vaultId
            AND transaction_type = CAST('DEPOSIT' AS transaction_type)
            AND status = CAST('COMPLETED' AS transaction_status)
            """, nativeQuery = true)
    BigDecimal sumCompletedDeposits(@Param("vaultId") UUID vaultId);

    // ── Filter by transaction type ────────────────────────────────────────────
    // Native query — type passed as plain string, cast to PostgreSQL enum
    @Query(value = """
            SELECT * FROM transactions
            WHERE vault_id = :vaultId
            AND transaction_type = CAST(:type AS transaction_type)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Transaction> findByVaultIdAndType(
            @Param("vaultId") UUID vaultId,
            @Param("type") String type);

    // ── Filter by status ──────────────────────────────────────────────────────
    // Native query — status passed as plain string, cast to PostgreSQL enum
    @Query(value = """
            SELECT * FROM transactions
            WHERE vault_id = :vaultId
            AND status = CAST(:status AS transaction_status)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Transaction> findByVaultIdAndStatusNative(
            @Param("vaultId") UUID vaultId,
            @Param("status") String status);

    // ── Pending transactions older than a threshold ───────────────────────────
    // Useful for a future cleanup job to detect stuck payments
    @Query(value = """
            SELECT * FROM transactions
            WHERE status = CAST('PENDING' AS transaction_status)
            AND created_at < NOW() - INTERVAL '1 hour'
            """, nativeQuery = true)
    List<Transaction> findStuckPendingTransactions();
}