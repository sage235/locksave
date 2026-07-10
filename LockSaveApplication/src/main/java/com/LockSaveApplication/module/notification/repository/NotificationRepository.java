// module/notification/repository/NotificationRepository.java

package com.LockSaveApplication.module.notification.repository;

import com.LockSaveApplication.module.notification.entity.Notification;
import com.LockSaveApplication.module.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    // Native query with explicit cast — fixes PostgreSQL enum vs varchar mismatch
    @Query(value = """
            SELECT * FROM notifications
            WHERE status = CAST(:status AS notification_status)
            """, nativeQuery = true)
    List<Notification> findByStatusNative(@Param("status") String status);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") UUID id, @Param("userId") UUID userId);
}