// module/payment/service/AirtelService.java

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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirtelService {

    private final WebClient.Builder   webClientBuilder;
    private final PaymentConfig       paymentConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_CACHE_KEY = "airtel:access_token";

    public PaymentResult requestDeposit(MomoPaymentRequest request) {
        try {
            String accessToken = getAccessToken();

            WebClient client = webClientBuilder
                    .baseUrl(paymentConfig.getAirtel().getBaseUrl())
                    .build();

            Map response = client.post()
                    .uri("/merchant/v2/payments/")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Country", "RW")
                    .header("X-Currency", "RWF")
                    .bodyValue(Map.of(
                        "reference",   request.getTransactionReference(),
                        "subscriber", Map.of(
                            "country", "RW",
                            "currency", "RWF",
                            "msisdn",  request.getPayerPhone()
                        ),
                        "transaction", Map.of(
                            "amount",   request.getAmount(),
                            "country",  "RW",
                            "currency", "RWF",
                            "id",       request.getTransactionReference()
                        )
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String providerRef = extractNestedValue(response, "data", "transaction", "id");

            log.info("Airtel deposit initiated. providerRef: {}", providerRef);

            return PaymentResult.builder()
                    .success(true)
                    .providerReference(providerRef)
                    .build();

        } catch (Exception e) {
            log.error("Airtel deposit failed: {}", e.getMessage(), e);
            return PaymentResult.builder()
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    private String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null) return cached;

        WebClient client = webClientBuilder
                .baseUrl(paymentConfig.getAirtel().getBaseUrl())
                .build();

        Map response = client.post()
                .uri("/auth/oauth2/token")
                .bodyValue(Map.of(
                    "client_id",     paymentConfig.getAirtel().getClientId(),
                    "client_secret", paymentConfig.getAirtel().getClientSecret(),
                    "grant_type",    "client_credentials"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String token = (String) response.get("access_token");
        Integer expiresIn = (Integer) response.get("expires_in");

        redisTemplate.opsForValue().set(
                TOKEN_CACHE_KEY, token,
                expiresIn - 60, TimeUnit.SECONDS);

        return token;
    }

    @SuppressWarnings("unchecked")
    private String extractNestedValue(Map map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map) current).get(key);
            } else return null;
        }
        return current != null ? current.toString() : null;
    }
}