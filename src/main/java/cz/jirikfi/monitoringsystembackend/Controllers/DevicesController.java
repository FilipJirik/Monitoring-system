package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Recipients.RecipientResponse;
import cz.jirikfi.monitoringsystembackend.Models.Recipients.SetRecipientRequest;
import cz.jirikfi.monitoringsystembackend.Models.Thresholds.CreateThresholdRequest;
import cz.jirikfi.monitoringsystembackend.Models.Thresholds.ThresholdResponse;
import cz.jirikfi.monitoringsystembackend.Services.AlertRecipientService;
import cz.jirikfi.monitoringsystembackend.Services.AlertThresholdService;
import cz.jirikfi.monitoringsystembackend.Services.AuthorizationService;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DevicesController {
    private final DeviceService deviceService;
    private final AuthorizationService authorizationService;
    private final AlertThresholdService alertThresholdService;
    private final AlertRecipientService alertRecipientService;

    // POST /api/devices createDevice
    @PostMapping
    public ResponseEntity<DeviceWithApiKeyModel> createDevice(
            @RequestBody @Valid CreateDeviceModel model,
            @AuthenticationPrincipal UUID userId) {
        DeviceWithApiKeyModel device = deviceService.createDevice(userId, model);

        return ResponseEntity.status(HttpStatus.CREATED).body(device);
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
    public ResponseEntity<Void> deleteDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, id);

        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
    // POST /api/devices/{id}/image
    @PostMapping("/{id}/image")
    public ResponseEntity<Void> uploadPicture(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, id);

        deviceService.changeDevicePicture(id, file);
        return ResponseEntity.noContent().build();
    }
    // DELETE /api/devices/{id}/image
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deletePicture(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId
    ) {
        authorizationService.checkDeviceWritePermission(userId, id);
        deviceService.resetDevicePicture(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/devices/regenerate-api-key?name= Regenerate current api key of device with name
    @PostMapping("/regenerate-api-key")
    public ResponseEntity<DeviceWithApiKeyModel> regenerateApiKey(
            @RequestParam String name,
            @AuthenticationPrincipal UUID userId) {

        DeviceWithApiKeyModel model = deviceService.regenerateApiKeyByName(userId, name);
        return ResponseEntity.ok(model);
    }

    // GET /api/devices/{id} GET device by id
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceAccess(userId, id);

        DeviceResponse device = deviceService.getDevice(id);
        return ResponseEntity.ok().body(device);
    }

    // GET /api/devices GET all devices
    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices(
            @AuthenticationPrincipal UUID userId) {

        List<DeviceResponse> devices = deviceService.getAllAccessibleDevices(userId);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{deviceId}/thresholds")
    public ResponseEntity<List<ThresholdResponse>> getThresholds(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceAccess(userId, deviceId);
        List<ThresholdResponse> thresholds = alertThresholdService.getThresholdsByDeviceId(deviceId);
        return ResponseEntity.ok(thresholds);
    }

    @PostMapping("/{deviceId}/thresholds")
    public ResponseEntity<ThresholdResponse> createThreshold(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateThresholdRequest request,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceWritePermission(userId, deviceId);
        ThresholdResponse threshold = alertThresholdService.createThreshold(deviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(threshold);
    }

    @GetMapping("/{deviceId}/recipients")
    public ResponseEntity<List<RecipientResponse>> getRecipients(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UUID userId) {

        List<RecipientResponse> recipients = alertRecipientService.getRecipientsByDeviceId(deviceId, userId);
        return ResponseEntity.ok(recipients);
    }

    @PutMapping("/{deviceId}/recipients")
    public ResponseEntity<RecipientResponse> setRecipient(
            @PathVariable UUID deviceId,
            @Valid @RequestBody SetRecipientRequest request,
            @AuthenticationPrincipal UUID userId) {

        RecipientResponse recipient = alertRecipientService.setRecipient(deviceId, userId, request);
        return ResponseEntity.ok(recipient);
    }

    @DeleteMapping("/{deviceId}/recipients/{recipientUserId}")
    public ResponseEntity<Void> deleteRecipient(
            @PathVariable UUID deviceId,
            @PathVariable UUID recipientUserId,
            @AuthenticationPrincipal UUID userId) {

        alertRecipientService.deleteRecipient(deviceId, recipientUserId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/devices/owned
     * Only devices where user is OWNER
     *
     * If query has parameter ?name=..., return API key model
     * If query does not have parameter, return without API keys models
     */
//    @GetMapping("/owned")
//    public ResponseEntity<?> getOwnedDevices(
//            @RequestParam(required = false) String name,
//            @AuthenticationPrincipal UUID userId) {
//
//        if (name != null && !name.isBlank()) {
//            DeviceWithApiKeyModel device = deviceService.getOwnedDeviceByName(userId, name);
//            return ResponseEntity.ok(device);
//        }
//
//        List<DeviceResponse> devices = deviceService.getOwnedDevices(userId);
//        return ResponseEntity.ok(devices);
//    }

}