// module/notification/entity/Notification.java

package com.LockSaveApplication.module.notification.entity;

import com.LockSaveApplication.module.notification.enums.*;
import com.LockSaveApplication.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // columnDefinition tells Hibernate to use the PostgreSQL
    // native enum type instead of varchar — fixes the cast error
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "notification_type")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "notification_channel")
    private NotificationChannel channel;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "notification_status")
    private NotificationStatus status;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}