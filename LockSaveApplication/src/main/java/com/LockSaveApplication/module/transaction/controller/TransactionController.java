// module/transaction/controller/TransactionController.java

package com.LockSaveApplication.module.transaction.controller;

import com.LockSaveApplication.common.response.ApiResponse;
import com.LockSaveApplication.module.transaction.dto.*;
import com.LockSaveApplication.module.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateDeposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DepositRequest request) {

        TransactionResponse response = transactionService
                .initiateDeposit(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit initiated", response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> initiateWithdrawal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request) {

        TransactionResponse response = transactionService
                .initiateWithdrawal(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal initiated", response));
    }

    @GetMapping("/vault/{vaultId}")
    public ResponseEntity<ApiResponse<TransactionPageResponse>> getVaultTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID vaultId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionPageResponse response = transactionService
                .getVaultTransactions(userDetails.getUsername(), vaultId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", response));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID transactionId) {

        TransactionResponse response = transactionService
                .getTransaction(userDetails.getUsername(), transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved", response));
    }
}