package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.DevicePrincipal;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.MetricPeriod;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsHistoryDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateRequestDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsStatusDto;
import cz.jirikfi.monitoringsystembackend.services.MetricsCollectingService;
import cz.jirikfi.monitoringsystembackend.services.MetricsReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsReadingService metricsReadingService;
    private final MetricsCollectingService metricsCollectingService;

    @PostMapping
    public ResponseEntity<Void> receiveMetrics(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal DevicePrincipal devicePrincipal,
            @Valid @RequestBody MetricsCreateRequestDto model) {

        metricsCollectingService.processIncomingMetrics(deviceId, devicePrincipal.getDeviceId(), model);
        return ResponseEntity.accepted().build(); // faster but without acknowledgment
    }

    @GetMapping("/latest")
    public ResponseEntity<MetricsStatusDto> getLatestMetrics(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        MetricsStatusDto metrics = metricsReadingService.getLatestMetrics(principal, deviceId);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/history")
    public ResponseEntity<MetricsHistoryDto> getMetricsHistory(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam MetricType type,
            @RequestParam(defaultValue = "HOUR_24") MetricPeriod period
    ) {
        MetricsHistoryDto history = metricsReadingService.getMetricsHistory(principal, deviceId, type, period);
        return ResponseEntity.ok(history);
    }
}
