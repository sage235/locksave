// module/vault/service/VaultService.java

package com.LockSaveApplication.module.vault.service;

import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import com.LockSaveApplication.module.vault.dto.*;
import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import com.LockSaveApplication.module.vault.mapper.VaultMapper;
import com.LockSaveApplication.module.vault.repository.VaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultRepository vaultRepository;
    private final UserRepository  userRepository;
    private final VaultMapper     vaultMapper;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public VaultResponse createVault(String email, CreateVaultRequest request) {
        User user = getUser(email);

        Vault vault = Vault.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .goalAmount(request.getGoalAmount())
                .currentBalance(java.math.BigDecimal.ZERO)
                .unlockDate(request.getUnlockDate())
                .status(VaultStatus.ACTIVE)
                .build();

        Vault saved = vaultRepository.save(vault);
        log.info("Vault created: {} for user: {}", saved.getId(), email);
        return vaultMapper.toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VaultResponse> getUserVaults(String email) {
        User user = getUser(email);
        return vaultRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(vaultMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VaultResponse getVault(String email, UUID vaultId) {
        Vault vault = getVaultForUser(email, vaultId);
        return vaultMapper.toResponse(vault);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public VaultResponse updateVault(String email, UUID vaultId,
                                     UpdateVaultRequest request) {
        Vault vault = getVaultForUser(email, vaultId);

        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new IllegalStateException("Cannot update a closed vault.");
        }

        if (request.getTitle() != null) {
            vault.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            vault.setDescription(request.getDescription());
        }
        if (request.getUnlockDate() != null) {
            // can only extend, never shorten
            if (request.getUnlockDate().isBefore(vault.getUnlockDate())) {
                throw new IllegalArgumentException(
                    "New unlock date cannot be earlier than the current unlock date.");
            }
            vault.setUnlockDate(request.getUnlockDate());
        }

        return vaultMapper.toResponse(vaultRepository.save(vault));
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    @Transactional
    public void closeVault(String email, UUID vaultId) {
        Vault vault = getVaultForUser(email, vaultId);

        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new IllegalStateException("Vault is already closed.");
        }
        if (vault.isLocked()) {
            throw new IllegalStateException(
                "Cannot close a locked vault. Wait until the unlock date.");
        }
        if (vault.getCurrentBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                "Cannot close a vault with remaining balance. Withdraw funds first.");
        }

        vault.setStatus(VaultStatus.CLOSED);
        vaultRepository.save(vault);
        log.info("Vault closed: {} by user: {}", vaultId, email);
    }

    // ── Scheduled unlock ──────────────────────────────────────────────────────

    @Transactional
    public void unlockDueVaults() {
        List<Vault> due = vaultRepository.findVaultsDueForUnlock(
                LocalDate.now(), VaultStatus.ACTIVE);

        due.forEach(vault -> {
            vault.setStatus(VaultStatus.UNLOCKED);
            log.info("Vault auto-unlocked: {}", vault.getId());
        });

        vaultRepository.saveAll(due);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Vault getVaultForUser(String email, UUID vaultId) {
        User user = getUser(email);
        return vaultRepository.findByIdAndUserId(vaultId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vault", "id", vaultId));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
    }
}