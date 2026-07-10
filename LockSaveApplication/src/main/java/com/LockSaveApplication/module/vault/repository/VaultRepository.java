// module/vault/repository/VaultRepository.java

package com.LockSaveApplication.module.vault.repository;

import com.LockSaveApplication.module.vault.entity.Vault;
import com.LockSaveApplication.module.vault.enums.VaultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaultRepository extends JpaRepository<Vault, UUID> {

    List<Vault> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Vault> findByIdAndUserId(UUID vaultId, UUID userId);

    boolean existsByIdAndUserId(UUID vaultId, UUID userId);

    long countByUserId(UUID userId);

    // Finds vaults whose unlock date has passed but status is still ACTIVE
    // — used by a scheduled job to flip them to UNLOCKED
 
@Query(value = """
        SELECT * FROM vaults
        WHERE unlock_date <= :today
        AND status = CAST(:status AS vault_status)
        """, nativeQuery = true)
List<Vault> findVaultsDueForUnlock(
        @Param("today") java.time.LocalDate today,
        @Param("status") String active);
        }
