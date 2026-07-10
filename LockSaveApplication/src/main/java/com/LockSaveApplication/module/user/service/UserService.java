// module/user/service/UserService.java

package com.LockSaveApplication.module.user.service;

import com.LockSaveApplication.common.exception.DuplicateResourceException;
import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.module.user.dto.UserProfileRequest;
import com.LockSaveApplication.module.user.dto.UserProfileResponse;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Get profile ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = getUser(email);
        return toResponse(user);
    }

    // ── Update profile ────────────────────────────────────────────────────────

    @Transactional
    public UserProfileResponse updateProfile(String email,
                                              UserProfileRequest request) {
        User user = getUser(email);

        if (request.getFullName() != null
                && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().isBlank()
                && !request.getPhoneNumber().equals(user.getPhoneNumber())) {

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new DuplicateResourceException(
                        "User", "phone number", request.getPhoneNumber());
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for: {}", email);
        return toResponse(saved);
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Transactional
    public void changePassword(String email,
                                String currentPassword,
                                String newPassword) {
        User user = getUser(email);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must be different from current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for: {}", email);
    }

    // ── Deactivate account ────────────────────────────────────────────────────

    @Transactional
    public void deactivateAccount(String email) {
        User user = getUser(email);
        user.setActive(false);
        userRepository.save(user);
        log.warn("Account deactivated: {}", email);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .emailVerified(user.isEmailVerified())
                .active(user.isActive())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}