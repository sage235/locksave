// module/transaction/dto/WithdrawalRequest.java

package com.LockSaveApplication.module.transaction.dto;

import com.LockSaveApplication.module.transaction.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class WithdrawalRequest {

    @NotNull(message = "Vault ID is required")
    private UUID vaultId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum withdrawal is 1.00")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Destination phone for Mobile Money withdrawal
    @Pattern(
        regexp = "^\\+?[1-9]\\d{7,14}$",
        message = "Invalid phone number format"
    )
    private String destinationPhone;

    // Destination bank account for card withdrawal
    private String bankAccountNumber;
}