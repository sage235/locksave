// module/payment/dto/PaymentResult.java

package com.LockSaveApplication.module.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResult {
    private boolean success;
    private String  providerReference;
    private String  failureReason;
}