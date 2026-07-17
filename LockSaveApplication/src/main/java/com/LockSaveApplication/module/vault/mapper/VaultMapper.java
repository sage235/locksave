// module/vault/mapper/VaultMapper.java

package com.LockSaveApplication.module.vault.mapper;

import com.LockSaveApplication.common.util.DateUtil;
import com.LockSaveApplication.module.vault.dto.VaultResponse;
import com.LockSaveApplication.module.vault.entity.Vault;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaultMapper {

    private final DateUtil dateUtil;

    public VaultResponse toResponse(Vault vault) {
        return VaultResponse.builder()
                .id(vault.getId())
                .title(vault.getTitle())
                .description(vault.getDescription())
                .goalAmount(vault.getGoalAmount())
                .currentBalance(vault.getCurrentBalance())
                .progressPercent(vault.progressPercent())
                .unlockDate(vault.getUnlockDate())
                .daysUntilUnlock(dateUtil.daysUntilUnlock(vault.getUnlockDate()))
                .locked(vault.isLocked())
                .status(vault.getStatus())
                .createdAt(vault.getCreatedAt())
                .build();
    }
}