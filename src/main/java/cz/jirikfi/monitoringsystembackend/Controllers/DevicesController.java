package cz.jirikfi.monitoringsystembackend.Controllers;


import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Services.AuthorizationService;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/// Origin for accessing information about stored devices

@RestController
@RequestMapping("/api/devices")
public class DevicesController {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private AuthorizationService authorizationService;

    // POST /api/devices createDevice
    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @RequestBody @Valid CreateDeviceModel model,
            @AuthenticationPrincipal UUID userId) {
        DeviceResponse device = deviceService.createDevice(userId, model);

        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }
    // GET /api/devices/{id} getDevice
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceAccess(userId, id);

        DeviceResponse device = deviceService.getDevice(id);
        return ResponseEntity.ok().body(device);
    }
    // PUT /api/devices/{id} updateDevice
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDeviceModel model,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, id);

        DeviceResponse device = deviceService.updateDevice(id, model);
        return ResponseEntity.ok().body(device);
    }
    // DELETE /api/devices/{id} deleteDevice
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, id);

        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/devices/{id}/picture
    @PutMapping("/{id}/picture")
    public ResponseEntity<DeviceResponse> uploadPicture(
            @PathVariable UUID id,
            @RequestParam("picture") MultipartFile picture,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, id);

        DeviceResponse device = deviceService.changePicture(id, picture);
        return ResponseEntity.ok().body(device);
    }
    // POST /{id}/regenerate-api-key Regenate current api key
    @PostMapping("/{id}/regenerate-api-key")
    public ResponseEntity<String> regenerateApiKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceOwnership(userId, id);

        String newApiKey = deviceService.regenerateApiKey(id);
        return ResponseEntity.ok(newApiKey);
    }
    // GET /api/devices GET all user's devices
//    @GetMapping
//    public ResponseEntity<DeviceResponse> getAllDevices(
//            @AuthenticationPrincipal UUID userId) {
//
//        authorizationService.checkDeviceAccess(userId, id);
//
//        DeviceResponse device = deviceService.getDevice(id);
//        return ResponseEntity.ok().body(device);
//    } // TODO


    // Metrics
    // GET /devices/{id}/metrics  GET all metrics of device
    @GetMapping("{id}/metrics")
    public ResponseEntity<List<Metrics>> getMetrics(@PathVariable UUID id) {
        List<Metrics> metrics = metricsService.getMetrics(id);
        return ResponseEntity.ok().body(metrics);
    } // TODO add API KEY validation
}