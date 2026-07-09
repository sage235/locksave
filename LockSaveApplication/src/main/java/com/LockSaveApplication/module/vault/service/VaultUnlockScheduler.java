// module/vault/service/VaultUnlockScheduler.java

package com.LockSaveApplication.module.vault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VaultUnlockScheduler {

    private final VaultService vaultService;

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void unlockDueVaults() {
        log.info("Running vault unlock scheduler...");
        vaultService.unlockDueVaults();
    }
}