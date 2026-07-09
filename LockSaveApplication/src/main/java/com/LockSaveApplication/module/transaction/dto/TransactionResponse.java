// module/transaction/dto/TransactionResponse.java

package com.LockSaveApplication.module.transaction.dto;

import com.LockSaveApplication.module.transaction.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponse {
    private UUID              id;
    private UUID              vaultId;
    private String            vaultTitle;
    private TransactionType   transactionType;
    private BigDecimal        amount;
    private PaymentMethod     paymentMethod;
    private TransactionStatus status;
    private String            transactionReference;
    private String            providerReference;
    private String            failureReason;
    private LocalDateTime     createdAt;
}