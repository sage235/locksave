// module/vault/controller/VaultController.java

package com.LockSaveApplication.module.vault.controller;

import com.LockSaveApplication.common.response.ApiResponse;
import com.LockSaveApplication.module.vault.dto.*;
import com.LockSaveApplication.module.vault.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vaults")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @PostMapping
    public ResponseEntity<ApiResponse<VaultResponse>> createVault(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVaultRequest request) {

        VaultResponse response = vaultService.createVault(
                userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vault created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VaultResponse>>> getUserVaults(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<VaultResponse> vaults = vaultService.getUserVaults(
                userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Vaults retrieved", vaults));
    }

    @GetMapping("/{vaultId}")
    public ResponseEntity<ApiResponse<VaultResponse>> getVault(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID vaultId) {

        VaultResponse response = vaultService.getVault(
                userDetails.getUsername(), vaultId);
        return ResponseEntity.ok(ApiResponse.success("Vault retrieved", response));
    }

    @PatchMapping("/{vaultId}")
    public ResponseEntity<ApiResponse<VaultResponse>> updateVault(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID vaultId,
            @Valid @RequestBody UpdateVaultRequest request) {

        VaultResponse response = vaultService.updateVault(
                userDetails.getUsername(), vaultId, request);
        return ResponseEntity.ok(ApiResponse.success("Vault updated", response));
    }

    @DeleteMapping("/{vaultId}")
    public ResponseEntity<ApiResponse<Void>> closeVault(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID vaultId) {

        vaultService.closeVault(userDetails.getUsername(), vaultId);
        return ResponseEntity.ok(ApiResponse.success("Vault closed successfully"));
    }
}