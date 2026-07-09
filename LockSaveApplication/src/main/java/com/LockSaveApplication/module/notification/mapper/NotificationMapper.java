// module/notification/mapper/NotificationMapper.java

package com.LockSaveApplication.module.notification.mapper;

import com.LockSaveApplication.module.notification.dto.NotificationResponse;
import com.LockSaveApplication.module.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .read(notification.isRead())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}