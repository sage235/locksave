// test/java/com/locksave/transaction/TransactionServiceTest.java

package com.LockSaveApplication.auth.vault.transaction;

import com.LockSaveApplication.common.exception.InsufficientFundsException;
import com.LockSaveApplication.common.exception.VaultLockedException;
import com.LockSaveApplication.common.util.IdempotencyUtil;
import com.LockSaveApplication.module.transaction.dto.DepositRequest;
import com.LockSaveApplication.module.transaction.dto.TransactionResponse;
import com.LockSaveApplication.module.transaction.dto.WithdrawalRequest;
import com.LockSaveApplication.module.transaction.entity.Transaction;
import com.LockSaveApplication.module.transaction.enums.PaymentMethod;
import com.LockSaveApplication.module.transaction.enums.TransactionStatus;
import com.LockSaveApplication.module.transaction.enums.TransactionType;
import com.LockSaveApplication.module.transaction.mapper.TransactionMapper;
import com.LockSaveApplication.module.transaction.repository.TransactionRepository;
import com.LockSaveApplication.module.transaction.service.TransactionService;
import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
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
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock VaultService          vaultService;
    @Mock TransactionMapper     transactionMapper;
    @Mock IdempotencyUtil       idempotencyUtil;

    @InjectMocks TransactionService transactionService;

    private Vault lockedVault;
    private Vault unlockedVault;

    @BeforeEach
    void setUp() {
        lockedVault = Vault.builder()
                .id(UUID.randomUUID())
                .title("Tuition Fund")
                .currentBalance(new BigDecimal("300000"))
                .unlockDate(LocalDate.now().plusMonths(3))
                .status(VaultStatus.ACTIVE)
                .build();

        unlockedVault = Vault.builder()
                .id(UUID.randomUUID())
                .title("Emergency Fund")
                .currentBalance(new BigDecimal("100000"))
                .unlockDate(LocalDate.now().minusDays(1))
                .status(VaultStatus.UNLOCKED)
                .build();
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Test
    void initiateDeposit_success_returnsPendingTransaction() {
        DepositRequest request = new DepositRequest();
        request.setVaultId(lockedVault.getId());
        request.setAmount(new BigDecimal("50000"));
        request.setPaymentMethod(PaymentMethod.MTN_MOMO);
        request.setPayerPhone("+250788000000");

        Transaction saved = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(lockedVault)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .status(TransactionStatus.PENDING)
                .transactionReference("DEP-ABC123")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        when(vaultService.getVaultForUser(any(), any())).thenReturn(lockedVault);
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(idempotencyUtil.generate()).thenReturn(UUID.randomUUID().toString());
        when(transactionRepository.save(any())).thenReturn(saved);
        when(transactionMapper.toResponse(any())).thenReturn(mock(TransactionResponse.class));

        TransactionResponse response = transactionService
                .initiateDeposit("jean@locksave.rw", request);

        assertThat(response).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void initiateDeposit_duplicateIdempotencyKey_returnsExistingTransaction() {
        DepositRequest request = new DepositRequest();
        request.setVaultId(lockedVault.getId());
        request.setAmount(new BigDecimal("50000"));
        request.setPaymentMethod(PaymentMethod.MTN_MOMO);
        request.setIdempotencyKey("existing-key");

        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(lockedVault)
                .status(TransactionStatus.PENDING)
                .transactionReference("DEP-EXISTING")
                .idempotencyKey("existing-key")
                .build();

        when(vaultService.getVaultForUser(any(), any())).thenReturn(lockedVault);
        when(transactionRepository.existsByIdempotencyKey("existing-key")).thenReturn(true);
        when(transactionRepository.findByIdempotencyKey("existing-key"))
                .thenReturn(Optional.of(existing));
        when(transactionMapper.toResponse(any())).thenReturn(mock(TransactionResponse.class));

        transactionService.initiateDeposit("jean@locksave.rw", request);

        // should NOT save a new transaction
        verify(transactionRepository, never()).save(any());
    }

    // ── Withdrawal ────────────────────────────────────────────────────────────

    @Test
    void initiateWithdrawal_lockedVault_throwsVaultLockedException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setVaultId(lockedVault.getId());
        request.setAmount(new BigDecimal("50000"));
        request.setPaymentMethod(PaymentMethod.MTN_MOMO);

        when(vaultService.getVaultForUser(any(), any())).thenReturn(lockedVault);

        assertThatThrownBy(() ->
                transactionService.initiateWithdrawal("jean@locksave.rw", request))
                .isInstanceOf(VaultLockedException.class);
    }

    @Test
    void initiateWithdrawal_insufficientFunds_throwsInsufficientFundsException() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setVaultId(unlockedVault.getId());
        request.setAmount(new BigDecimal("999999")); // more than balance
        request.setPaymentMethod(PaymentMethod.MTN_MOMO);

        when(vaultService.getVaultForUser(any(), any())).thenReturn(unlockedVault);

        assertThatThrownBy(() ->
                transactionService.initiateWithdrawal("jean@locksave.rw", request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void initiateWithdrawal_success_deductsBalance() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setVaultId(unlockedVault.getId());
        request.setAmount(new BigDecimal("50000"));
        request.setPaymentMethod(PaymentMethod.MTN_MOMO);
        request.setDestinationPhone("+250788000000");

        Transaction saved = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(unlockedVault)
                .amount(new BigDecimal("50000"))
                .status(TransactionStatus.PENDING)
                .transactionReference("WIT-ABC123")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        when(vaultService.getVaultForUser(any(), any())).thenReturn(unlockedVault);
        when(idempotencyUtil.generate()).thenReturn(UUID.randomUUID().toString());
        when(transactionRepository.save(any())).thenReturn(saved);
        when(transactionMapper.toResponse(any())).thenReturn(mock(TransactionResponse.class));

        transactionService.initiateWithdrawal("jean@locksave.rw", request);

        // balance should be reduced
        assertThat(unlockedVault.getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    // ── Confirm deposit ───────────────────────────────────────────────────────

    @Test
    void confirmDeposit_updatesBalanceAndStatus() {
        Transaction pending = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(lockedVault)
                .amount(new BigDecimal("50000"))
                .status(TransactionStatus.PENDING)
                .transactionReference("DEP-ABC")
                .build();

        when(transactionRepository.findByTransactionReference("DEP-ABC"))
                .thenReturn(Optional.of(pending));
        when(transactionRepository.save(any())).thenReturn(pending);

        transactionService.confirmDeposit("DEP-ABC", "MTN-REF-001");

        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(lockedVault.getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("350000"));
    }

    @Test
    void confirmDeposit_alreadyCompleted_isIdempotent() {
        Transaction completed = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(lockedVault)
                .amount(new BigDecimal("50000"))
                .status(TransactionStatus.COMPLETED)
                .transactionReference("DEP-ABC")
                .build();

        when(transactionRepository.findByTransactionReference("DEP-ABC"))
                .thenReturn(Optional.of(completed));

        // calling confirm twice should not throw and should not save again
        assertThatNoException()
                .isThrownBy(() -> transactionService.confirmDeposit("DEP-ABC", "MTN-REF-001"));

        verify(transactionRepository, never()).save(any());
    }

    // ── Fail withdrawal ───────────────────────────────────────────────────────

    @Test
    void failWithdrawal_refundsBalanceToVault() {
        BigDecimal originalBalance = unlockedVault.getCurrentBalance();
        BigDecimal withdrawalAmount = new BigDecimal("30000");

        // simulate balance already deducted
        unlockedVault.setCurrentBalance(originalBalance.subtract(withdrawalAmount));

        Transaction pending = Transaction.builder()
                .id(UUID.randomUUID())
                .vault(unlockedVault)
                .amount(withdrawalAmount)
                .status(TransactionStatus.PENDING)
                .transactionReference("WIT-ABC")
                .build();

        when(transactionRepository.findByTransactionReference("WIT-ABC"))
                .thenReturn(Optional.of(pending));
        when(transactionRepository.save(any())).thenReturn(pending);

        transactionService.failWithdrawal("WIT-ABC", "Provider timeout");

        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(unlockedVault.getCurrentBalance())
                .isEqualByComparingTo(originalBalance);
    }
}