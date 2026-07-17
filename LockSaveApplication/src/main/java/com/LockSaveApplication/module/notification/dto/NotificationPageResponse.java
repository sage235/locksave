// module/notification/dto/NotificationPageResponse.java

package com.LockSaveApplication.module.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationPageResponse {
    private List<NotificationResponse> notifications;
    private long                       unreadCount;
    private int                        page;
    private int                        size;
    private long                       totalElements;
    private int                        totalPages;
    private boolean                    last;
}