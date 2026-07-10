// module/user/dto/UserProfileRequest.java

package com.LockSaveApplication.module.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {

    @Size(min = 2, max = 100,
          message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(
        regexp = "^\\+?[1-9]\\d{7,14}$",
        message = "Invalid phone number format"
    )
    private String phoneNumber;
}