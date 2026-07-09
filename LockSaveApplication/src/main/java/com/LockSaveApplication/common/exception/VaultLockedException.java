package com.LockSaveApplication.common.exception;

import java.time.LocalDate;

public class VaultLockedException extends RuntimeException {
    public VaultLockedException(LocalDate unlockDate) {
        super(String.format(
            "This vault is locked until %s. Withdrawals are not permitted before this date.",
            unlockDate
        ));
    }
}