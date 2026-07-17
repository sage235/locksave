// test/java/com/locksave/auth/AuthServiceTest.java

package com.LockSaveApplication.auth;

import com.LockSaveApplication.common.exception.DuplicateResourceException;
import com.LockSaveApplication.module.auth.dto.LoginRequest;
import com.LockSaveApplication.module.auth.dto.OtpVerifyRequest;
import com.LockSaveApplication.module.auth.dto.RegisterRequest;
import com.LockSaveApplication.module.auth.service.AuthService;
import com.LockSaveApplication.module.auth.service.OtpService;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import com.LockSaveApplication.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtTokenProvider      jwtTokenProvider;
    @Mock OtpService            otpService;
    @Mock JavaMailSender        mailSender;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User            existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Jean Doe");
        registerRequest.setEmail("jean@locksave.rw");
        registerRequest.setPassword("Password@123");
        registerRequest.setPhoneNumber("+250788000000");

        existingUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Doe")
                .email("jean@locksave.rw")
                .passwordHash("hashed")
                .phoneNumber("+250788000000")
                .isEmailVerified(true)
                .isActive(true)
                .role("USER")
                .build();
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(existingUser);
        when(otpService.generateAndStore(any())).thenReturn("123456");

        assertThatNoException().isThrownBy(() -> authService.register(registerRequest));

        verify(userRepository).save(any(User.class));
        verify(otpService).generateAndStore("jean@locksave.rw");
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("jean@locksave.rw")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicatePhone_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("phone");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jean@locksave.rw");
        loginRequest.setPassword("Password@123");

        when(userRepository.findByEmail("jean@locksave.rw"))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(any(), any()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any()))
                .thenReturn("refresh-token");

        var response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("jean@locksave.rw");
    }

    @Test
    void login_emailNotVerified_throwsBadCredentialsAndSendsOtp() {
        existingUser.setEmailVerified(false);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("jean@locksave.rw");
        loginRequest.setPassword("Password@123");

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(existingUser));
        when(otpService.generateAndStore(any())).thenReturn("123456");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not verified");

        verify(otpService).generateAndStore("jean@locksave.rw");
    }

    // ── OTP ───────────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_success_setsEmailVerified() {
        existingUser.setEmailVerified(false);

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setEmail("jean@locksave.rw");
        request.setOtp("123456");

        when(userRepository.findByEmail("jean@locksave.rw"))
                .thenReturn(Optional.of(existingUser));

        authService.verifyOtp(request);

        assertThat(existingUser.isEmailVerified()).isTrue();
        verify(userRepository).save(existingUser);
    }
}