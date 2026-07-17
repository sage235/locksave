// module/vault/entity/Vault.java

package com.LockSaveApplication.module.vault.entity;

import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaults")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(name = "goal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal goalAmount;

    @Builder.Default
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "unlock_date", nullable = false)
    private LocalDate unlockDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "vault_status")
    private VaultStatus status = VaultStatus.ACTIVE;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Domain logic ──────────────────────────────────────────────────────────

    /**
     * Computed — never stored.
     * A vault is locked if the unlock date is in the future.
     */
    public boolean isLocked() {
        return LocalDate.now().isBefore(this.unlockDate);
    }

    /**
     * Progress toward goal as a percentage (0–100).
     */
    public int progressPercent() {
        if (goalAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return currentBalance
                .multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, java.math.RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }
}