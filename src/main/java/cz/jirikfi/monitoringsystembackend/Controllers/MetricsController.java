package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Models.Metrics.MetricsModel;
import cz.jirikfi.monitoringsystembackend.Services.AuthorizationService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;
    private final AuthorizationService authorizationService;

    private static final String API_KEY_HEADER = "X-API-KEY"; // FIXME: use modern standard

    @PostMapping
    public ResponseEntity<?> receiveMetrics(
            @PathVariable UUID deviceId,
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody MetricsModel model) {

        metricsService.saveMetrics(deviceId, apiKey, model);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Slice<MetricsModel>> getMetricsHistory(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal User user,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size) {

        authorizationService.checkDeviceAccess(user.getId(), deviceId);

        Slice<MetricsModel> metrics = metricsService.getMetricsHistory(deviceId, PageRequest.of(page, size));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/latest")
    public ResponseEntity<MetricsModel> getLatestMetrics(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal User user) {

        authorizationService.checkDeviceAccess(user.getId(), deviceId);

        MetricsModel latest = metricsService.getLatestMetrics(deviceId);
        return ResponseEntity.ok(latest);
    }

    // GET /api/devices/{id}/getAll/cpu
    // GET /api/devices/{id}/getAll/{RAM|CPU|GPU|NETWORK|DISK|BATTERY}
    // GET only RAM | CPU | GPU | NETWORK | DISK | BATTERY metrics by device id
    // -> for single longterm graphs

    // USER can choose from modes like (AVERAGE OF DAY, WEEK, OF MONTH) to make average of old records
}
