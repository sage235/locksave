// module/notification/controller/NotificationController.java

package com.LockSaveApplication.module.notification.controller;

import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.common.response.ApiResponse;
import com.LockSaveApplication.module.notification.dto.NotificationPageResponse;
import com.LockSaveApplication.module.notification.service.NotificationService;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository      userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = getUser(userDetails.getUsername());
        NotificationPageResponse response = notificationService
                .getUserNotifications(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", response));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID notificationId) {

        User user = getUser(userDetails.getUsername());
        notificationService.markAsRead(notificationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails.getUsername());
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", email));
    }
}