// module/vault/dto/VaultResponse.java

package com.LockSaveApplication.module.vault.dto;

import com.LockSaveApplication.module.vault.enums.VaultStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class VaultResponse {
    private UUID        id;
    private String      title;
    private String      description;
    private BigDecimal  goalAmount;
    private BigDecimal  currentBalance;
    private int         progressPercent;
    private LocalDate   unlockDate;
    private long        daysUntilUnlock;
    private boolean     locked;
    private VaultStatus status;
    private LocalDateTime createdAt;
}