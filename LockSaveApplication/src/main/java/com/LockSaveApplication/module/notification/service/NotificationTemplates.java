// module/notification/service/NotificationTemplates.java

package com.LockSaveApplication.module.notification.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Centralizes all notification message templates.
 * Keeps service logic clean and templates easy to update.
 */
@Component
public class NotificationTemplates {

    public String depositConfirmedTitle() {
        return "Deposit Confirmed";
    }

    public String depositConfirmedBody(String fullName, BigDecimal amount,
                                        String vaultTitle, BigDecimal newBalance) {
        return String.format(
            "Hello %s,\n\nYour deposit of %s RWF into \"%s\" has been confirmed.\n" +
            "New vault balance: %s RWF.\n\nKeep saving!\n\nThe LockSave Team",
            fullName, amount.toPlainString(), vaultTitle, newBalance.toPlainString()
        );
    }

    public String withdrawalCompletedTitle() {
        return "Withdrawal Completed";
    }

    public String withdrawalCompletedBody(String fullName, BigDecimal amount,
                                           String vaultTitle) {
        return String.format(
            "Hello %s,\n\nYour withdrawal of %s RWF from \"%s\" has been completed.\n\n" +
            "The LockSave Team",
            fullName, amount.toPlainString(), vaultTitle
        );
    }

    public String vaultUnlockedTitle() {
        return "Your Vault is Now Unlocked!";
    }

    public String vaultUnlockedBody(String fullName, String vaultTitle,
                                     BigDecimal balance) {
        return String.format(
            "Hello %s,\n\nGreat news! Your vault \"%s\" is now unlocked.\n" +
            "Available balance: %s RWF.\n\n" +
            "You can now withdraw your savings.\n\nThe LockSave Team",
            fullName, vaultTitle, balance.toPlainString()
        );
    }

    public String goalReachedTitle() {
        return "Savings Goal Reached!";
    }

    public String goalReachedBody(String fullName, String vaultTitle,
                                   BigDecimal goalAmount) {
        return String.format(
            "Hello %s,\n\nCongratulations! You have reached your savings goal of %s RWF " +
            "in \"%s\".\n\nYou are doing amazing!\n\nThe LockSave Team",
            fullName, goalAmount.toPlainString(), vaultTitle
        );
    }

    public String unlockReminderTitle() {
        return "Your Vault Unlocks Soon";
    }

    public String unlockReminderBody(String fullName, String vaultTitle,
                                      LocalDate unlockDate, long daysLeft) {
        return String.format(
            "Hello %s,\n\nYour vault \"%s\" will unlock in %d day(s) on %s.\n\n" +
            "Get ready to access your savings!\n\nThe LockSave Team",
            fullName, vaultTitle, daysLeft, unlockDate
        );
    }
}