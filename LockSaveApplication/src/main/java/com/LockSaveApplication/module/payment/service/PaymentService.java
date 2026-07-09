// module/payment/service/PaymentService.java

package com.LockSaveApplication.module.payment.service;

import com.LockSaveApplication.module.payment.dto.MomoPaymentRequest;
import com.LockSaveApplication.module.payment.dto.PaymentResult;
import com.LockSaveApplication.module.transaction.enums.PaymentMethod;
import com.LockSaveApplication.module.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MomoService        momoService;
    private final AirtelService      airtelService;
    private final TransactionService transactionService;

    // ── Dispatch deposit to correct provider ──────────────────────────────────

    public void processDeposit(String transactionReference,
                                BigDecimal amount,
                                String currency,
                                String payerPhone,
                                PaymentMethod method) {

        MomoPaymentRequest request = MomoPaymentRequest.builder()
                .transactionReference(transactionReference)
                .amount(amount)
                .currency(currency)
                .payerPhone(payerPhone)
                .payerMessage("LockSave deposit")
                .payeeNote("Vault deposit - " + transactionReference)
                .build();

        PaymentResult result = switch (method) {
            case MTN_MOMO    -> momoService.requestMtnDeposit(request);
            case AIRTEL_MONEY -> airtelService.requestDeposit(request);
            case ORANGE_MONEY -> handleOrangeDeposit(request);
            default -> throw new IllegalArgumentException(
                    "Payment method not supported for MoMo: " + method);
        };

        if (!result.isSuccess()) {
            transactionService.confirmDeposit(transactionReference, null);
            log.error("Deposit dispatch failed for reference: {}", transactionReference);
        }

        // Success path — wait for webhook to confirm
        log.info("Deposit dispatched to provider. reference: {}", transactionReference);
    }

    // ── Dispatch withdrawal to correct provider ───────────────────────────────

    public void processWithdrawal(String transactionReference,
                                   BigDecimal amount,
                                   String currency,
                                   String destinationPhone,
                                   PaymentMethod method) {

        MomoPaymentRequest request = MomoPaymentRequest.builder()
                .transactionReference(transactionReference)
                .amount(amount)
                .currency(currency)
                .payerPhone(destinationPhone)
                .payerMessage("LockSave withdrawal")
                .payeeNote("Vault withdrawal - " + transactionReference)
                .build();

        PaymentResult result = switch (method) {
            case MTN_MOMO    -> momoService.requestMtnWithdrawal(request);
            case AIRTEL_MONEY -> airtelService.requestDeposit(request); // disbursement endpoint
            default -> throw new IllegalArgumentException(
                    "Payment method not supported for withdrawal: " + method);
        };

        if (!result.isSuccess()) {
            transactionService.failWithdrawal(
                    transactionReference, result.getFailureReason());
        }
    }

    private PaymentResult handleOrangeDeposit(MomoPaymentRequest request) {
        // Orange Money integration follows same pattern as MTN/Airtel
        // Implement when Orange Money sandbox credentials are obtained
        log.warn("Orange Money integration pending. reference: {}",
                request.getTransactionReference());
        return PaymentResult.builder()
                .success(false)
                .failureReason("Orange Money integration coming soon")
                .build();
    }
}