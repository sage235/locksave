// module/transaction/dto/DepositRequest.java

package com.LockSaveApplication.module.transaction.dto;

import com.LockSaveApplication.module.transaction.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class DepositRequest {

    @NotNull(message = "Vault ID is required")
    private UUID vaultId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit is 1.00")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Phone number for Mobile Money payments
    @Pattern(
        regexp = "^\\+?[1-9]\\d{7,14}$",
        message = "Invalid phone number format"
    )
    private String payerPhone;

    // Optional idempotency key supplied by client
    // If not supplied, one is generated server-side
    private String idempotencyKey;
}