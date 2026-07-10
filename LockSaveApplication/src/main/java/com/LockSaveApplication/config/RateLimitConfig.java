// config/RateLimitConfig.java

package com.LockSaveApplication.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class RateLimitConfig {

    // Each IP gets its own bucket per endpoint group
    // ConcurrentHashMap is thread-safe for concurrent requests
    private final Map<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> otpBuckets      = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets  = new ConcurrentHashMap<>();

    // ── Login bucket ──────────────────────────────────────────
    // Max 5 login attempts per IP per 15 minutes
    // Protects against brute force on credentials
    public Bucket resolveLoginBucket(String ipAddress) {
        return loginBuckets.computeIfAbsent(ipAddress, key -> {
            log.debug("Creating login rate limit bucket for IP: {}", key);
            return Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(5)
                            .refillIntervally(5, Duration.ofMinutes(15))
                            .build())
                    .build();
        });
    }

    // ── OTP bucket ────────────────────────────────────────────
    // Max 3 OTP requests per IP per 10 minutes
    // Protects against OTP flooding and SMS cost abuse
    public Bucket resolveOtpBucket(String ipAddress) {
        return otpBuckets.computeIfAbsent(ipAddress, key -> {
            log.debug("Creating OTP rate limit bucket for IP: {}", key);
            return Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(3)
                            .refillIntervally(3, Duration.ofMinutes(10))
                            .build())
                    .build();
        });
    }

    // ── General API bucket ────────────────────────────────────
    // Max 100 requests per IP per minute
    // General protection for all other endpoints
    public Bucket resolveGeneralBucket(String ipAddress) {
        return generalBuckets.computeIfAbsent(ipAddress, key -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(100)
                        .refillIntervally(100, Duration.ofMinutes(1))
                        .build())
                .build());
    }
}