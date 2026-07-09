// module/payment/controller/PaymentWebhookController.java

package com.LockSaveApplication.module.payment.controller;

import com.LockSaveApplication.module.payment.dto.WebhookPayload;
import com.LockSaveApplication.module.payment.service.WebhookSignatureVerifier;
import com.LockSaveApplication.module.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final TransactionService      transactionService;
    private final WebhookSignatureVerifier signatureVerifier;

    // ── MTN MoMo webhook ──────────────────────────────────────────────────────

    @PostMapping("/mtn")
    public ResponseEntity<Void> handleMtnWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Callback-Signature", required = false)
                String signature) {

        log.info("MTN webhook received");

        if (!signatureVerifier.verify("MTN", rawPayload, signature)) {
            log.warn("MTN webhook signature verification failed");
            return ResponseEntity.status(401).build();
        }

        // parse and process
        WebhookPayload payload = parsePayload(rawPayload);
        processWebhook(payload, "MTN");

        // Always return 200 quickly — provider may retry on non-200
        return ResponseEntity.ok().build();
    }

    // ── Airtel webhook ────────────────────────────────────────────────────────

    @PostMapping("/airtel")
    public ResponseEntity<Void> handleAirtelWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Signature", required = false)
                String signature) {

        log.info("Airtel webhook received");

        if (!signatureVerifier.verify("AIRTEL", rawPayload, signature)) {
            log.warn("Airtel webhook signature verification failed");
            return ResponseEntity.status(401).build();
        }

        WebhookPayload payload = parsePayload(rawPayload);
        processWebhook(payload, "AIRTEL");

        return ResponseEntity.ok().build();
    }

    // ── Orange webhook ────────────────────────────────────────────────────────

    @PostMapping("/orange")
    public ResponseEntity<Void> handleOrangeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Authorization", required = false)
                String signature) {

        log.info("Orange webhook received");

        if (!signatureVerifier.verify("ORANGE", rawPayload, signature)) {
            log.warn("Orange webhook signature verification failed");
            return ResponseEntity.status(401).build();
        }

        WebhookPayload payload = parsePayload(rawPayload);
        processWebhook(payload, "ORANGE");

        return ResponseEntity.ok().build();
    }

    // ── Shared processing logic ───────────────────────────────────────────────

    private void processWebhook(WebhookPayload payload, String provider) {
        try {
            boolean isSuccess = "SUCCESSFUL".equalsIgnoreCase(payload.getStatus())
                    || "SUCCESS".equalsIgnoreCase(payload.getStatus());

            if (isSuccess) {
                // Try deposit confirmation first
                // If the reference belongs to a withdrawal, confirm that instead
                try {
                    transactionService.confirmDeposit(
                            payload.getExternalId(),
                            payload.getFinancialTransactionId());
                } catch (Exception e) {
                    transactionService.confirmWithdrawal(
                            payload.getExternalId(),
                            payload.getFinancialTransactionId());
                }
            } else {
                transactionService.failWithdrawal(
                        payload.getExternalId(),
                        payload.getReason());
            }

            log.info("{} webhook processed. reference: {} status: {}",
                    provider, payload.getExternalId(), payload.getStatus());

        } catch (Exception e) {
            // Never throw from a webhook handler
            // Log the error but return 200 so the provider does not retry infinitely
            log.error("{} webhook processing error: {}", provider, e.getMessage(), e);
        }
    }

    private WebhookPayload parsePayload(String rawPayload) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
            return new WebhookPayload();
        }
    }
}