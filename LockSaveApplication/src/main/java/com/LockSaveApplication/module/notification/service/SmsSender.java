// module/notification/service/SmsSender.java

package com.LockSaveApplication.module.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSender {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.sms.api-url}")
    private String smsApiUrl;

    @Value("${app.sms.api-key}")
    private String smsApiKey;

    @Value("${app.sms.sender-id}")
    private String senderId;

    @Async
    public void send(String phone, String message) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(smsApiUrl)
                    .header("Authorization", "Bearer " + smsApiKey)
                    .bodyValue(Map.of(
                        "to",      phone,
                        "from",    senderId,
                        "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("SMS sent to: {}", phone);
        } catch (Exception e) {
            log.error("Failed to send SMS to: {} reason: {}", phone, e.getMessage());
            throw e;
        }
    }
}