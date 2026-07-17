// security/filter/RateLimitFilter.java

package com.LockSaveApplication.security.filter;

import com.LockSaveApplication.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper    objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip   = extractIp(request);
        String path = request.getRequestURI();

        Bucket bucket = resolveBucket(ip, path);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Pass remaining tokens as header so frontend can show feedback
            response.setHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded. IP: {} path: {} retry after: {}s",
                    ip, path, waitSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setHeader("X-Rate-Limit-Remaining", "0");

            objectMapper.writeValue(response.getOutputStream(), Map.of(
                "success",   false,
                "message",   "Too many requests. Please try again in "
                             + waitSeconds + " seconds.",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
        }
    }

    private Bucket resolveBucket(String ip, String path) {
        if (path.contains("/auth/login")) {
            return rateLimitConfig.resolveLoginBucket(ip);
        }
        if (path.contains("/auth/verify-otp")
                || path.contains("/auth/resend-otp")) {
            return rateLimitConfig.resolveOtpBucket(ip);
        }
        return rateLimitConfig.resolveGeneralBucket(ip);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}