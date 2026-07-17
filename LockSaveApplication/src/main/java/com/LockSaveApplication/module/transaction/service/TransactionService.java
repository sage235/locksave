// module/transaction/service/TransactionService.java

package com.LockSaveApplication.module.transaction.service;

import com.LockSaveApplication.common.exception.InsufficientFundsException;
import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.common.exception.VaultLockedException;
import com.LockSaveApplication.common.util.IdempotencyUtil;
import com.LockSaveApplication.module.transaction.dto.*;
import com.LockSaveApplication.module.transaction.entity.Transaction;
import com.LockSaveApplication.module.transaction.enums.*;
import com.LockSaveApplication.module.transaction.mapper.TransactionMapper;
import com.LockSaveApplication.module.transaction.repository.TransactionRepository;
import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import com.LockSaveApplication.module.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final VaultService          vaultService;
    private final TransactionMapper     transactionMapper;
    private final IdempotencyUtil       idempotencyUtil;

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse initiateDeposit(String email, DepositRequest request) {
        Vault vault = vaultService.getVaultForUser(email, request.getVaultId());

        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new IllegalStateException("Cannot deposit into a closed vault.");
        }

        // resolve idempotency key
        String idempotencyKey = resolveIdempotencyKey(request.getIdempotencyKey());

        // guard against duplicate requests
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(transactionMapper::toResponse)
                    .orElseThrow();
        }

        Transaction transaction = Transaction.builder()
                .vault(vault)
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .transactionReference(generateReference("DEP"))
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Deposit initiated: {} for vault: {}", saved.getId(), vault.getId());

        // At this point the payment module sends the request to MTN/Airtel/Orange
        // Vault balance is NOT updated here — only after webhook confirmation

        return transactionMapper.toResponse(saved);
    }

    // ── Webhook confirmation (called by PaymentService after provider confirms) ──

    @Transactional
    public void confirmDeposit(String transactionReference, String providerReference) {
        Transaction transaction = transactionRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", "reference", transactionReference));

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            log.warn("Deposit already confirmed: {}", transactionReference);
            return; // idempotent — safe to call twice
        }

        Vault vault = transaction.getVault();
        vault.setCurrentBalance(
                vault.getCurrentBalance().add(transaction.getAmount()));

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProviderReference(providerReference);

        transactionRepository.save(transaction);

        log.info("Deposit confirmed: {} vault balance now: {}",
                transactionReference, vault.getCurrentBalance());
    }

    // ── Withdrawal ────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse initiateWithdrawal(String email,
                                                   WithdrawalRequest request) {
        Vault vault = vaultService.getVaultForUser(email, request.getVaultId());

        // enforce time lock
        if (vault.isLocked()) {
            throw new VaultLockedException(vault.getUnlockDate());
        }

        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new IllegalStateException("Cannot withdraw from a closed vault.");
        }

        // enforce sufficient balance
        if (vault.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    request.getAmount(), vault.getCurrentBalance());
        }

        // reserve the funds immediately — deduct on initiation not on confirmation
        // this prevents double-spend if two withdrawal requests arrive simultaneously
        vault.setCurrentBalance(
                vault.getCurrentBalance().subtract(request.getAmount()));

        Transaction transaction = Transaction.builder()
                .vault(vault)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .transactionReference(generateReference("WIT"))
                .idempotencyKey(idempotencyUtil.generate())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Withdrawal initiated: {} for vault: {}", saved.getId(), vault.getId());

        return transactionMapper.toResponse(saved);
    }

    @Transactional
    public void confirmWithdrawal(String transactionReference,
                                   String providerReference) {
        Transaction transaction = transactionRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", "reference", transactionReference));

        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            log.warn("Withdrawal already confirmed: {}", transactionReference);
            return;
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProviderReference(providerReference);
        transactionRepository.save(transaction);

        log.info("Withdrawal confirmed: {}", transactionReference);
    }

    @Transactional
    public void failWithdrawal(String transactionReference, String reason) {
        Transaction transaction = transactionRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", "reference", transactionReference));

        // refund the reserved amount back to vault
        Vault vault = transaction.getVault();
        vault.setCurrentBalance(
                vault.getCurrentBalance().add(transaction.getAmount()));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transactionRepository.save(transaction);

        log.warn("Withdrawal failed: {} reason: {}", transactionReference, reason);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionPageResponse getVaultTransactions(String email,
                                                         UUID vaultId,
                                                         int page,
                                                         int size) {
        vaultService.getVaultForUser(email, vaultId); // ownership check

        Page<Transaction> result = transactionRepository
                .findByVaultIdOrderByCreatedAtDesc(vaultId, PageRequest.of(page, size));

        return buildPageResponse(result);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String email, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction", "id", transactionId));

        // ownership check — verify the vault belongs to this user
        vaultService.getVaultForUser(email, transaction.getVault().getId());

        return transactionMapper.toResponse(transaction);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveIdempotencyKey(String clientKey) {
        return (clientKey != null && !clientKey.isBlank())
                ? clientKey
                : idempotencyUtil.generate();
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private TransactionPageResponse buildPageResponse(Page<Transaction> page) {
        return TransactionPageResponse.builder()
                .transactions(page.getContent().stream()
                        .map(transactionMapper::toResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}