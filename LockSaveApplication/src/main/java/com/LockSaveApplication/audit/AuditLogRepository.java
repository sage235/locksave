// audit/AuditLogRepository.java

package com.LockSaveApplication.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<AuditLog> findByActionAndCreatedAtBetween(
            AuditAction action,
            LocalDateTime from,
            LocalDateTime to);
}