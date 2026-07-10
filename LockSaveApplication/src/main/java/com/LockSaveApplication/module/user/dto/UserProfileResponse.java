// module/user/dto/UserProfileResponse.java

package com.LockSaveApplication.module.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {
    private UUID          id;
    private String        fullName;
    private String        email;
    private String        phoneNumber;
    private boolean       emailVerified;
    private boolean       active;
    private String        role;
    private LocalDateTime createdAt;
}