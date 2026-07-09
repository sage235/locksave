// common/util/DateUtil.java

package com.LockSaveApplication.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DateUtil {

    /**
     * Returns true if the vault is still locked (unlock date is in the future).
     */
    public boolean isLocked(LocalDate unlockDate) {
        return LocalDate.now().isBefore(unlockDate);
    }

    /**
     * Returns number of days remaining until unlock.
     * Returns 0 if already unlocked.
     */
    public long daysUntilUnlock(LocalDate unlockDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), unlockDate);
        return Math.max(days, 0);
    }

    /**
     * Returns true if the unlock date has been reached.
     */
    public boolean isUnlocked(LocalDate unlockDate) {
        return !LocalDate.now().isBefore(unlockDate);
    }
}