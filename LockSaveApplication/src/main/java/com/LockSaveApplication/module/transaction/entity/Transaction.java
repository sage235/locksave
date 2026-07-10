// module/transaction/entity/Transaction.java

package com.LockSaveApplication.module.transaction.entity;

import com.LockSaveApplication.module.transaction.enums.*;
import com.LockSaveApplication.module.vault.entity.Vault;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_id", nullable = false)
    private Vault vault;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false,
            columnDefinition = "transaction_type")
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false,
            columnDefinition = "payment_method")
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private TransactionStatus status = TransactionStatus.PENDING;
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 100)
    private String transactionReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}