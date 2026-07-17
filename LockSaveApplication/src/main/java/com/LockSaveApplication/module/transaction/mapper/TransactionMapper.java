// module/transaction/mapper/TransactionMapper.java

package com.LockSaveApplication.module.transaction.mapper;

import com.LockSaveApplication.module.transaction.dto.TransactionResponse;
import com.LockSaveApplication.module.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .vaultId(transaction.getVault().getId())
                .vaultTitle(transaction.getVault().getTitle())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .transactionReference(transaction.getTransactionReference())
                .providerReference(transaction.getProviderReference())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}