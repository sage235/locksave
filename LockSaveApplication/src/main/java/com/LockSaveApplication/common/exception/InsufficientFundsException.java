package com.LockSaveApplication.common.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal requested, BigDecimal available) {
        super(String.format(
            "Insufficient funds. Requested: %s, Available: %s",
            requested, available
        ));
    }
}