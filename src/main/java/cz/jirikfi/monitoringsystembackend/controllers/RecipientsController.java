package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.models.recipients.CreateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.UpdateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.services.AlertRecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/recipients")
@RequiredArgsConstructor
public class RecipientsController {

    private final AlertRecipientService alertRecipientService;

    // ALERT RECIPIENTS

    // POST /api/devices/{id}/recipients add recipient for alert for device
    @PostMapping
    public ResponseEntity<RecipientResponseDto> addRecipient(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateRecipientRequestDto model,
            @AuthenticationPrincipal UserPrincipal principal) {

        RecipientResponseDto response = alertRecipientService.addRecipient(principal, deviceId, model);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/devices/{id}/recipients?all=true
    @GetMapping()
    public ResponseEntity<Page<RecipientStatusDto>> getRecipients(
            @PathVariable UUID deviceId,
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean includeAll,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<RecipientStatusDto> result = alertRecipientService.getRecipientsWithStatus(principal, deviceId, includeAll, pageable);
        return ResponseEntity.ok(result);
    }

    // PUT /api/devices/{id}/recipients/{recipientId} change recipient for alert for device
    @PutMapping("/{recipientUserId}")
    public ResponseEntity<RecipientResponseDto> updateRecipient(
            @PathVariable UUID deviceId,
            @PathVariable UUID recipientUserId,
            @Valid @RequestBody UpdateRecipientRequestDto model,
            @AuthenticationPrincipal UserPrincipal principal) {

        RecipientResponseDto response = alertRecipientService.updateRecipient(principal, deviceId, recipientUserId, model);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/devices/{id}/recipients/{recipientUserId} delete recipient for alert for device
    @DeleteMapping("/{recipientUserId}")
    public ResponseEntity<Void> deleteRecipient(
            @PathVariable UUID deviceId,
            @PathVariable UUID recipientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {

        alertRecipientService.deleteRecipient(principal, deviceId, recipientUserId);
        return ResponseEntity.noContent().build();
    }
}
