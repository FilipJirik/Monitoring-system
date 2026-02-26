package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.models.thresholds.CreateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.ThresholdResponseDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.UpdateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.services.AlertThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/thresholds")
@RequiredArgsConstructor
public class ThresholdsController {

    private final AlertThresholdService alertThresholdService;

    // GET /api/devices/{deviceId}/thresholds
    @GetMapping
    public ResponseEntity<List<ThresholdResponseDto>> getThresholds(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(alertThresholdService.getThresholdsByDeviceId(principal, deviceId));
    }

    // POST /api/devices/{deviceId}/thresholds
    @PostMapping
    public ResponseEntity<ThresholdResponseDto> createThreshold(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateThresholdRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alertThresholdService.createThreshold(principal, deviceId, request));
    }

    // PUT /api/devices/{deviceId}/thresholds/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ThresholdResponseDto> updateThreshold(
            @PathVariable UUID deviceId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThresholdRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(alertThresholdService.updateThreshold(principal, deviceId, id, request));
    }

    // DELETE /api/devices/{deviceId}/thresholds/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(
            @PathVariable UUID deviceId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        alertThresholdService.deleteThreshold(principal, deviceId, id);
        return ResponseEntity.noContent().build();
    }
}
