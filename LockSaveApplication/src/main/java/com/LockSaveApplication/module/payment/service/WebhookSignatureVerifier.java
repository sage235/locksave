// module/payment/service/WebhookSignatureVerifier.java

package com.LockSaveApplication.module.payment.service;

import com.LockSaveApplication.config.PaymentConfig;
import com.LockSaveApplication.module.payment.dto.WebhookVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

    private final PaymentConfig paymentConfig;

    /**
     * Verifies the HMAC-SHA256 signature on an incoming webhook.
     * Each provider sends a signature in a request header.
     * If the computed signature does not match, the webhook is rejected.
     */
    public boolean verify(String provider, String payload, String signature) {
        try {
            String secret = resolveSecret(provider);
            String computed = computeHmac(payload, secret);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed for provider: {}", provider, e);
            return false;
        }
    }

    private String computeHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private String resolveSecret(String provider) {
        return switch (provider.toUpperCase()) {
            case "MTN"    -> paymentConfig.getWebhook().getMtnSecret();
            case "AIRTEL" -> paymentConfig.getWebhook().getAirtelSecret();
            case "ORANGE" -> paymentConfig.getWebhook().getOrangeSecret();
            default -> throw new IllegalArgumentException(
                    "Unknown provider: " + provider);
        };
    }
}