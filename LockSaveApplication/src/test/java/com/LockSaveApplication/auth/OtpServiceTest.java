// test/java/com/locksave/auth/OtpServiceTest.java

package com.LockSaveApplication.auth;

import com.LockSaveApplication.common.exception.OtpException;
import com.LockSaveApplication.module.auth.service.OtpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock StringRedisTemplate  redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks OtpService otpService;

    @Test
    void generateAndStore_returnsOtpOf6Digits() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doNothing().when(valueOps).set(any(), any(), anyLong(), any());

        String otp = otpService.generateAndStore("jean@locksave.rw");

        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void verify_validOtp_doesNotThrow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:attempts:jean@locksave.rw")).thenReturn(null);
        when(valueOps.get("otp:jean@locksave.rw")).thenReturn("123456");

        assertThatNoException()
                .isThrownBy(() -> otpService.verify("jean@locksave.rw", "123456"));

        verify(redisTemplate).delete("otp:jean@locksave.rw");
        verify(redisTemplate).delete("otp:attempts:jean@locksave.rw");
    }

    @Test
    void verify_expiredOtp_throwsOtpException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(contains("attempts"))).thenReturn(null);
        when(valueOps.get("otp:jean@locksave.rw")).thenReturn(null);

        assertThatThrownBy(() -> otpService.verify("jean@locksave.rw", "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verify_wrongOtp_throwsOtpException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(contains("attempts"))).thenReturn(null);
        when(valueOps.get("otp:jean@locksave.rw")).thenReturn("999999");

        assertThatThrownBy(() -> otpService.verify("jean@locksave.rw", "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void verify_tooManyAttempts_throwsOtpException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(contains("attempts"))).thenReturn("3");

        assertThatThrownBy(() -> otpService.verify("jean@locksave.rw", "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("Too many");
    }
}