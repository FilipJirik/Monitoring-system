package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertResponseModel;
import cz.jirikfi.monitoringsystembackend.services.AlertService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertResponseModel>> getAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean isResolved,
            @RequestParam(required = false) AlertSeverity severity,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(alertService.getAlerts(principal, isResolved, severity, pageable));
    }
    // POST /api/alerts/{id}/resolve
    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertResponseModel> resolveAlert(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(alertService.resolveAlert(id, principal));
    }
}