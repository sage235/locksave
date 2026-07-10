// test/java/com/locksave/vault/VaultServiceTest.java

package com.LockSaveApplication.auth.vault;

import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import com.LockSaveApplication.module.vault.dto.CreateVaultRequest;
import com.LockSaveApplication.module.vault.dto.UpdateVaultRequest;
import com.LockSaveApplication.module.vault.dto.VaultResponse;
import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import com.LockSaveApplication.module.vault.mapper.VaultMapper;
import com.LockSaveApplication.module.vault.repository.VaultRepository;
import com.LockSaveApplication.module.vault.service.VaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock VaultRepository vaultRepository;
    @Mock UserRepository  userRepository;
    @Mock VaultMapper     vaultMapper;

    @InjectMocks VaultService vaultService;

    private User  user;
    private Vault lockedVault;
    private Vault unlockedVault;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("jean@locksave.rw")
                .fullName("Jean Doe")
                .build();

        lockedVault = Vault.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Laptop Fund")
                .goalAmount(new BigDecimal("500000"))
                .currentBalance(BigDecimal.ZERO)
                .unlockDate(LocalDate.now().plusMonths(6))
                .status(VaultStatus.ACTIVE)
                .build();

        unlockedVault = Vault.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Emergency Fund")
                .goalAmount(new BigDecimal("200000"))
                .currentBalance(new BigDecimal("200000"))
                .unlockDate(LocalDate.now().minusDays(1))
                .status(VaultStatus.UNLOCKED)
                .build();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createVault_success() {
        CreateVaultRequest request = new CreateVaultRequest();
        request.setTitle("Laptop Fund");
        request.setGoalAmount(new BigDecimal("500000"));
        request.setUnlockDate(LocalDate.now().plusMonths(6));

        when(userRepository.findByEmail("jean@locksave.rw"))
                .thenReturn(Optional.of(user));
        when(vaultRepository.save(any())).thenReturn(lockedVault);
        when(vaultMapper.toResponse(any())).thenReturn(mock(VaultResponse.class));

        VaultResponse response = vaultService.createVault("jean@locksave.rw", request);

        assertThat(response).isNotNull();
        verify(vaultRepository).save(any(Vault.class));
    }

    // ── Lock behaviour ────────────────────────────────────────────────────────

    @Test
    void isLocked_futureUnlockDate_returnsTrue() {
        assertThat(lockedVault.isLocked()).isTrue();
    }

    @Test
    void isLocked_pastUnlockDate_returnsFalse() {
        assertThat(unlockedVault.isLocked()).isFalse();
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    @Test
    void closeVault_lockedVault_throwsIllegalState() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(vaultRepository.findByIdAndUserId(any(), any()))
                .thenReturn(Optional.of(lockedVault));

        assertThatThrownBy(() ->
                vaultService.closeVault("jean@locksave.rw", lockedVault.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void closeVault_remainingBalance_throwsIllegalState() {
        unlockedVault.setCurrentBalance(new BigDecimal("50000"));

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(vaultRepository.findByIdAndUserId(any(), any()))
                .thenReturn(Optional.of(unlockedVault));

        assertThatThrownBy(() ->
                vaultService.closeVault("jean@locksave.rw", unlockedVault.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("balance");
    }

    @Test
    void closeVault_unlockedEmptyVault_success() {
        unlockedVault.setCurrentBalance(BigDecimal.ZERO);

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(vaultRepository.findByIdAndUserId(any(), any()))
                .thenReturn(Optional.of(unlockedVault));
        when(vaultRepository.save(any())).thenReturn(unlockedVault);

        assertThatNoException().isThrownBy(() ->
                vaultService.closeVault("jean@locksave.rw", unlockedVault.getId()));

        assertThat(unlockedVault.getStatus()).isEqualTo(VaultStatus.CLOSED);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void updateVault_shortenUnlockDate_throwsIllegalArgument() {
        UpdateVaultRequest request = new UpdateVaultRequest();
        request.setUnlockDate(lockedVault.getUnlockDate().minusDays(10));

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(vaultRepository.findByIdAndUserId(any(), any()))
                .thenReturn(Optional.of(lockedVault));

        assertThatThrownBy(() ->
                vaultService.updateVault("jean@locksave.rw", lockedVault.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("earlier");
    }
}