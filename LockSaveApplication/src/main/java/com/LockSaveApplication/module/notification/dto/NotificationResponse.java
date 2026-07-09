// module/notification/dto/NotificationResponse.java

package com.LockSaveApplication.module.notification.dto;

import com.LockSaveApplication.module.notification.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID                id;
    private NotificationType    type;
    private NotificationChannel channel;
    private String              title;
    private String              message;
    private NotificationStatus  status;
    private boolean             read;
    private LocalDateTime       sentAt;
    private LocalDateTime       createdAt;
}