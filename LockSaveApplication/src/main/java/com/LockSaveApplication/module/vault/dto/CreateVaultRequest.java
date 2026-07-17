// module/vault/dto/CreateVaultRequest.java

package com.LockSaveApplication.module.vault.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateVaultRequest {

    @NotBlank(message = "Vault title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Goal amount is required")
    @DecimalMin(value = "1.00", message = "Goal amount must be at least 1.00")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    private BigDecimal goalAmount;

    @NotNull(message = "Unlock date is required")
    @Future(message = "Unlock date must be in the future")
    private LocalDate unlockDate;
}