// module/vault/dto/UpdateVaultRequest.java

package com.LockSaveApplication.module.vault.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateVaultRequest {

    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Future(message = "Unlock date must be in the future")
    private LocalDate unlockDate;
}
