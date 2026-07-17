// module/user/controller/UserController.java

package com.LockSaveApplication.module.user.controller;

import com.LockSaveApplication.common.response.ApiResponse;
import com.LockSaveApplication.module.user.dto.ChangePasswordRequest;
import com.LockSaveApplication.module.user.dto.UserProfileRequest;
import com.LockSaveApplication.module.user.dto.UserProfileResponse;
import com.LockSaveApplication.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse response = userService
                .getProfile(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileRequest request) {

        UserProfileResponse response = userService
                .updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated", response));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        userService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Account deactivated successfully"));
    }
}