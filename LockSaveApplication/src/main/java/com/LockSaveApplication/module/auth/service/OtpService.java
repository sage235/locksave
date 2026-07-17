// module/auth/service/OtpService.java

package com.LockSaveApplication.module.auth.service;

import com.LockSaveApplication.common.exception.OtpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int    OTP_LENGTH      = 6;
    private static final long   OTP_EXPIRY_MIN  = 5;
    private static final int    MAX_ATTEMPTS    = 3;
    private static final String OTP_PREFIX      = "otp:";
    private static final String ATTEMPT_PREFIX  = "otp:attempts:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom        secureRandom = new SecureRandom();

    public String generateAndStore(String email) {
        String otp = String.format("%0" + OTP_LENGTH + "d",
                secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH)));

        redisTemplate.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                OTP_EXPIRY_MIN,
                TimeUnit.MINUTES
        );

        // reset attempt counter on new OTP
        redisTemplate.delete(ATTEMPT_PREFIX + email);

        log.debug("OTP generated for email: {}", email);
        return otp;
    }

    public void verify(String email, String submittedOtp) {
        // check attempt count
        String attemptsStr = redisTemplate.opsForValue().get(ATTEMPT_PREFIX + email);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            throw new OtpException("Too many failed attempts. Please request a new OTP.");
        }

        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);

        if (storedOtp == null) {
            throw new OtpException("OTP expired or not found. Please request a new one.");
        }

        if (!storedOtp.equals(submittedOtp)) {
            redisTemplate.opsForValue().increment(ATTEMPT_PREFIX + email);
            throw new OtpException("Invalid OTP.");
        }

        // success — delete both keys immediately
        redisTemplate.delete(OTP_PREFIX + email);
        redisTemplate.delete(ATTEMPT_PREFIX + email);
    }
}