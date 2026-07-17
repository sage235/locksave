// module/payment/dto/WebhookVerificationResult.java

package com.LockSaveApplication.module.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookVerificationResult {
    private boolean valid;
    private String  provider;
    private String  transactionReference;
    private String  providerReference;
    private String  status;
    private String  failureReason;
}