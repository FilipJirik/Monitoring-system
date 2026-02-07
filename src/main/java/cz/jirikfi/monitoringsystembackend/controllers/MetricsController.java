package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.MetricPeriod;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsHistoryModel;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateModel;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsStatusModel;
import cz.jirikfi.monitoringsystembackend.services.AlertService;
import cz.jirikfi.monitoringsystembackend.services.AuthorizationService;
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
    private final MetricsCollectingService metricsIngresService;
    private final AuthorizationService authorizationService;
    private final AlertService alertService;

    private static final String API_KEY_HEADER = "X-API-KEY"; // FIXME: use modern standard

    @PostMapping
    public ResponseEntity<Void> receiveMetrics(
            @PathVariable UUID deviceId,
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody MetricsCreateModel model) {

        Metrics metricEntity = metricsIngresService.saveMetrics(deviceId, apiKey, model);
        alertService.checkThresholdsAsync(metricEntity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<MetricsHistoryModel> getMetricsHistory(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal User user,
            @RequestParam MetricType type,
            @RequestParam(defaultValue = "HOUR_24") MetricPeriod period
    ) {
        authorizationService.checkDeviceAccess(user.getId(), deviceId);

        return ResponseEntity.ok(metricsReadingService.getMetricsHistory(deviceId, type, period));
    }

    @GetMapping("/latest")
    public ResponseEntity<MetricsStatusModel> getLatestMetrics(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal User user) {

        authorizationService.checkDeviceAccess(user.getId(), deviceId);

        MetricsStatusModel latest = metricsReadingService.getLatestMetrics(deviceId);
        return ResponseEntity.ok(latest);
    }

    // GET /api/devices/{id}/getAll/cpu
    // GET /api/devices/{id}/getAll/{RAM|CPU|GPU|NETWORK|DISK|BATTERY}
    // GET only RAM | CPU | GPU | NETWORK | DISK | BATTERY metrics by device id
    // -> for single longterm graphs

    // USER can choose from modes like (AVERAGE OF DAY, WEEK, OF MONTH) to make average of old records
}
