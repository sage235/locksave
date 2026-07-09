// module/payment/dto/WebhookPayload.java

package com.LockSaveApplication.module.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookPayload {
    private String externalId;       // our transactionReference
    private String financialTransactionId; // provider's reference
    private String status;           // SUCCESSFUL, FAILED, etc.
    private String reason;
    private String provider;         // MTN, AIRTEL, ORANGE
}