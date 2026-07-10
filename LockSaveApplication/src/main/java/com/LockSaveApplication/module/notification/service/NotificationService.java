// module/notification/service/NotificationService.java

package com.LockSaveApplication.module.notification.service;

import com.LockSaveApplication.module.notification.dto.*;
import com.LockSaveApplication.module.notification.entity.Notification;
import com.LockSaveApplication.module.notification.enums.*;
import com.LockSaveApplication.module.notification.mapper.NotificationMapper;
import com.LockSaveApplication.module.notification.repository.NotificationRepository;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import com.LockSaveApplication.module.vault.repository.VaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper     notificationMapper;
    private final NotificationTemplates  templates;
    private final EmailSender            emailSender;
    private final SmsSender              smsSender;
    private final VaultRepository        vaultRepository;

    // ── Event-triggered notifications ─────────────────────────────────────────

    public void notifyDepositConfirmed(User user, Vault vault, BigDecimal amount) {
        String title = templates.depositConfirmedTitle();
        String body  = templates.depositConfirmedBody(
                user.getFullName(), amount,
                vault.getTitle(), vault.getCurrentBalance());

        sendAndPersist(user, NotificationType.DEPOSIT_CONFIRMED, title, body);
        checkGoalReached(user, vault);
    }

    public void notifyWithdrawalCompleted(User user, Vault vault, BigDecimal amount) {
        String title = templates.withdrawalCompletedTitle();
        String body  = templates.withdrawalCompletedBody(
                user.getFullName(), amount, vault.getTitle());

        sendAndPersist(user, NotificationType.WITHDRAWAL_COMPLETED, title, body);
    }

    public void notifyVaultUnlocked(User user, Vault vault) {
        String title = templates.vaultUnlockedTitle();
        String body  = templates.vaultUnlockedBody(
                user.getFullName(), vault.getTitle(), vault.getCurrentBalance());

        sendAndPersist(user, NotificationType.VAULT_UNLOCKED, title, body);
    }

    // ── Scheduled: unlock reminders (runs daily at 8am) ──────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendUnlockReminders() {
        // notify users whose vault unlocks in exactly 3 days
        LocalDate targetDate = LocalDate.now().plusDays(3);

       List<Vault> vaultsDueSoon = vaultRepository
        .findVaultsDueForUnlock(targetDate, VaultStatus.ACTIVE.name());

        vaultsDueSoon.forEach(vault -> {
            User user = vault.getUser();
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), vault.getUnlockDate());

            String title = templates.unlockReminderTitle();
            String body  = templates.unlockReminderBody(
                    user.getFullName(), vault.getTitle(),
                    vault.getUnlockDate(), daysLeft);

            sendAndPersist(user, NotificationType.ACCOUNT_ALERT, title, body);
            log.info("Unlock reminder sent for vault: {}", vault.getId());
        });
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public NotificationPageResponse getUserNotifications(UUID userId,
                                                          int page,
                                                          int size) {
        Page<Notification> result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return NotificationPageResponse.builder()
                .notifications(result.getContent().stream()
                        .map(notificationMapper::toResponse)
                        .toList())
                .unreadCount(unreadCount)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new com.LockSaveApplication.common.exception.ResourceNotFoundException(
                    "Notification", "id", notificationId);
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    // ── Retry failed notifications (runs every 30 minutes) ───────────────────

@Scheduled(fixedDelay = 1800000)
@Transactional
public void retryFailedNotifications() {
    // Use native query with string parameter — avoids enum cast error
    List<Notification> failed = notificationRepository
            .findByStatusNative(NotificationStatus.FAILED.name());

    failed.forEach(notification -> {
        try {
            dispatch(notification.getUser().getEmail(),
                     notification.getUser().getPhoneNumber(),
                     notification.getChannel(),
                     notification.getTitle(),
                     notification.getMessage());

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Retry succeeded for notification: {}", notification.getId());
        } catch (Exception e) {
            log.error("Retry failed for notification: {}", notification.getId());
        }
    });

    notificationRepository.saveAll(failed);
}


    // ── Internal helpers ──────────────────────────────────────────────────────

    private void sendAndPersist(User user, NotificationType type,
                                 String title, String body) {
        // persist IN_APP notification always
        persist(user, type, NotificationChannel.IN_APP,
                title, body, NotificationStatus.SENT);

        // send EMAIL
        sendViaChannel(user, type, title, body, NotificationChannel.EMAIL);

        // send SMS for financial events only
        if (isFinancialEvent(type)) {
            sendViaChannel(user, type, title, body, NotificationChannel.SMS);
        }
    }

    private void sendViaChannel(User user, NotificationType type,
                                 String title, String body,
                                 NotificationChannel channel) {
        Notification record = persist(user, type, channel,
                title, body, NotificationStatus.PENDING);
        try {
            dispatch(user.getEmail(), user.getPhoneNumber(), channel, title, body);
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            record.setStatus(NotificationStatus.FAILED);
            log.error("Notification dispatch failed. channel: {} user: {}",
                    channel, user.getEmail());
        }
        notificationRepository.save(record);
    }

    private Notification persist(User user, NotificationType type,
                                  NotificationChannel channel,
                                  String title, String body,
                                  NotificationStatus status) {
        return notificationRepository.save(
            Notification.builder()
                    .user(user)
                    .type(type)
                    .channel(channel)
                    .title(title)
                    .message(body)
                    .status(status)
                    .isRead(false)
                    .build()
        );
    }

    private void dispatch(String email, String phone,
                           NotificationChannel channel,
                           String title, String body) {
        switch (channel) {
            case EMAIL  -> emailSender.send(email, title, body);
            case SMS    -> smsSender.send(phone, body);
            case IN_APP -> { /* already persisted, no dispatch needed */ }
        }
    }

    private void checkGoalReached(User user, Vault vault) {
        if (vault.getCurrentBalance().compareTo(vault.getGoalAmount()) >= 0) {
            String title = templates.goalReachedTitle();
            String body  = templates.goalReachedBody(
                    user.getFullName(), vault.getTitle(), vault.getGoalAmount());
            sendAndPersist(user, NotificationType.GOAL_REACHED, title, body);
        }
    }

    private boolean isFinancialEvent(NotificationType type) {
        return type == NotificationType.DEPOSIT_CONFIRMED
                || type == NotificationType.WITHDRAWAL_COMPLETED
                || type == NotificationType.VAULT_UNLOCKED;
    }
}