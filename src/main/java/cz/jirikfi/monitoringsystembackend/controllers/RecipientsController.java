package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.models.recipients.CreateRecipientModel;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseModel;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusModel;
import cz.jirikfi.monitoringsystembackend.models.recipients.UpdateRecipientModel;
import cz.jirikfi.monitoringsystembackend.services.AlertRecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/recipients")
@RequiredArgsConstructor
public class RecipientsController {

    private final AlertRecipientService alertRecipientService;

    // ALERT RECIPIENTS

    // POST /api/devices/{id}/recipients add recipient for alert for device
    @PostMapping
    public ResponseEntity<RecipientResponseModel> addRecipient(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateRecipientModel model,
            @AuthenticationPrincipal UserPrincipal principal) {

        RecipientResponseModel response = alertRecipientService.addRecipient(principal, deviceId, model);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/devices/{id}/recipients?all=true
    @GetMapping
    public ResponseEntity<List<RecipientStatusModel>> getRecipients(
            @PathVariable UUID deviceId,
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean includeAll,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<RecipientStatusModel> result = alertRecipientService.getRecipientsWithStatus(principal, deviceId, includeAll);
        return ResponseEntity.ok(result);
    }

    // PUT /api/devices/{id}/recipients/{recipientId} change recipient for alert for device
    @PutMapping("/{recipientUserId}")
    public ResponseEntity<RecipientResponseModel> updateRecipient(
            @PathVariable UUID deviceId,
            @PathVariable UUID recipientUserId,
            @Valid @RequestBody UpdateRecipientModel model,
            @AuthenticationPrincipal UserPrincipal principal) {

        RecipientResponseModel response = alertRecipientService.updateRecipient(principal, deviceId, recipientUserId, model);
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
