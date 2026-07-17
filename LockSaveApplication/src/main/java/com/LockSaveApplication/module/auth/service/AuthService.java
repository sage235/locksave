// module/auth/service/AuthService.java

package com.LockSaveApplication.module.auth.service;

import com.LockSaveApplication.common.exception.DuplicateResourceException;
import com.LockSaveApplication.common.exception.ResourceNotFoundException;
import com.LockSaveApplication.module.auth.dto.*;
import com.LockSaveApplication.module.user.entity.User;
import com.LockSaveApplication.module.user.repository.UserRepository;
import com.LockSaveApplication.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtTokenProvider   jwtTokenProvider;
    private final OtpService         otpService;
    private final JavaMailSender     mailSender;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("User", "phone number", request.getPhoneNumber());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .isEmailVerified(false)
                .isActive(true)
                .role("USER")
                .build();

        userRepository.save(user);

        // send OTP
        String otp = otpService.generateAndStore(request.getEmail());
        sendOtpEmail(request.getEmail(), request.getFullName(), otp);

        log.info("User registered: {}", request.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.isEmailVerified()) {
            String otp = otpService.generateAndStore(user.getEmail());
            sendOtpEmail(user.getEmail(), user.getFullName(), otp);
            throw new BadCredentialsException(
                "Email not verified. A new OTP has been sent to " + user.getEmail());
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        otpService.verify(request.getEmail(), request.getOtp());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for: {}", request.getEmail());
    }

    public void resendOtp(OtpResendRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified.");
        }

        String otp = otpService.generateAndStore(request.getEmail());
        sendOtpEmail(request.getEmail(), user.getFullName(), otp);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900000) // 15 minutes in ms
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    private void sendOtpEmail(String email, String fullName, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("LockSave – Your Verification Code");
        message.setText(String.format(
            "Hello %s,\n\nYour verification code is: %s\n\nThis code expires in 5 minutes.\n\nDo not share this code with anyone.\n\nThe LockSave Team",
            fullName, otp
        ));
        mailSender.send(message);
    }
}