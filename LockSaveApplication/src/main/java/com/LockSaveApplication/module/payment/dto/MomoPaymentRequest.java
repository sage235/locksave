// module/payment/dto/MomoPaymentRequest.java

package com.LockSaveApplication.module.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MomoPaymentRequest {
    private String     transactionReference;
    private BigDecimal amount;
    private String     currency;
    private String     payerPhone;
    private String     payerMessage;
    private String     payeeNote;
}