// audit/AuditService.java

package com.LockSaveApplication.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(UUID userId,
                    AuditAction action,
                    String entityType,
                    UUID entityId,
                    String description,
                    HttpServletRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(extractIp(request))
                    .userAgent(request != null
                            ? request.getHeader("User-Agent") : null)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Audit logging must never break the main flow
            log.error("Failed to write audit log. action: {} error: {}",
                    action, e.getMessage());
        }
    }

    // Overload without request context (for scheduled jobs)
    @Async
    public void log(UUID userId,
                    AuditAction action,
                    String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .description(description)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log. action: {} error: {}",
                    action, e.getMessage());
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;

        // Check for IP behind a proxy or load balancer
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}