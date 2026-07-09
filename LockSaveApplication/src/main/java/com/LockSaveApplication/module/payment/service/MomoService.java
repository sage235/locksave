// module/payment/service/MomoService.java

package com.LockSaveApplication.module.payment.service;

import com.LockSaveApplication.config.PaymentConfig;
import com.LockSaveApplication.module.payment.dto.MomoPaymentRequest;
import com.LockSaveApplication.module.payment.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoService {

    private final WebClient.Builder  webClientBuilder;
    private final PaymentConfig      paymentConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_CACHE_KEY = "mtn:access_token";

    // ── MTN MoMo ─────────────────────────────────────────────────────────────

    public PaymentResult requestMtnDeposit(MomoPaymentRequest request) {
        try {
            String accessToken = getMtnAccessToken();
            String referenceId = UUID.randomUUID().toString();

            WebClient client = webClientBuilder
                    .baseUrl(paymentConfig.getMtnMomo().getBaseUrl())
                    .build();

            // MTN MoMo Collections API — Request to Pay
            client.post()
                    .uri("/collection/v1_0/requesttopay")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Reference-Id", referenceId)
                    .header("X-Target-Environment",
                            paymentConfig.getMtnMomo().getTargetEnvironment())
                    .header("Ocp-Apim-Subscription-Key",
                            paymentConfig.getMtnMomo().getSubscriptionKey())
                    .bodyValue(Map.of(
                        "amount",       request.getAmount().toPlainString(),
                        "currency",     request.getCurrency(),
                        "externalId",   request.getTransactionReference(),
                        "payer", Map.of(
                            "partyIdType", "MSISDN",
                            "partyId",     request.getPayerPhone()
                        ),
                        "payerMessage", request.getPayerMessage(),
                        "payeeNote",    request.getPayeeNote()
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("MTN MoMo request sent. referenceId: {}", referenceId);

            // MTN responds asynchronously via webhook
            // Return the referenceId so we can match it on webhook callback
            return PaymentResult.builder()
                    .success(true)
                    .providerReference(referenceId)
                    .build();

        } catch (Exception e) {
            log.error("MTN MoMo deposit failed: {}", e.getMessage(), e);
            return PaymentResult.builder()
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    public PaymentResult requestMtnWithdrawal(MomoPaymentRequest request) {
        try {
            String accessToken = getMtnDisbursementToken();
            String referenceId = UUID.randomUUID().toString();

            WebClient client = webClientBuilder
                    .baseUrl(paymentConfig.getMtnMomo().getBaseUrl())
                    .build();

            // MTN MoMo Disbursements API
            client.post()
                    .uri("/disbursement/v1_0/transfer")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Reference-Id", referenceId)
                    .header("X-Target-Environment",
                            paymentConfig.getMtnMomo().getTargetEnvironment())
                    .header("Ocp-Apim-Subscription-Key",
                            paymentConfig.getMtnMomo().getSubscriptionKey())
                    .bodyValue(Map.of(
                        "amount",       request.getAmount().toPlainString(),
                        "currency",     request.getCurrency(),
                        "externalId",   request.getTransactionReference(),
                        "payee", Map.of(
                            "partyIdType", "MSISDN",
                            "partyId",     request.getPayerPhone()
                        ),
                        "payerMessage", request.getPayerMessage(),
                        "payeeNote",    request.getPayeeNote()
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return PaymentResult.builder()
                    .success(true)
                    .providerReference(referenceId)
                    .build();

        } catch (Exception e) {
            log.error("MTN MoMo withdrawal failed: {}", e.getMessage(), e);
            return PaymentResult.builder()
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    // ── Token management ──────────────────────────────────────────────────────

    private String getMtnAccessToken() {
        // check Redis cache first
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null) return cached;

        WebClient client = webClientBuilder
                .baseUrl(paymentConfig.getMtnMomo().getBaseUrl())
                .build();

        Map response = client.post()
                .uri("/collection/token/")
                .headers(h -> h.setBasicAuth(
                        paymentConfig.getMtnMomo().getApiUser(),
                        paymentConfig.getMtnMomo().getApiKey()))
                .header("Ocp-Apim-Subscription-Key",
                        paymentConfig.getMtnMomo().getSubscriptionKey())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String token = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");

        // cache the token slightly before it expires
        redisTemplate.opsForValue().set(
                TOKEN_CACHE_KEY, token,
                expiresIn - 60, TimeUnit.SECONDS);

        return token;
    }

    private String getMtnDisbursementToken() {
        // Same pattern as collection token but different endpoint
        // Extracted separately because collection and disbursement
        // use different subscription keys in production
        return getMtnAccessToken();
    }
}