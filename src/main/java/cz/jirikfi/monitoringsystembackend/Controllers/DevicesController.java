package cz.jirikfi.monitoringsystembackend.Controllers;


import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Services.AuthorizationService;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DevicesController {

    private final DeviceService deviceService;
    private final MetricsService metricsService;
    private final AuthorizationService authorizationService;

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
    // POST /api/devices/{id}/regenerate-api-key Regenerate current api key
    @PostMapping("/{id}/regenerate-api-key")
    public ResponseEntity<String> regenerateApiKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        authorizationService.checkDeviceOwnership(userId, id);

        String newApiKey = deviceService.regenerateApiKey(id);
        return ResponseEntity.ok(newApiKey);
    }

    // POST /api/devices/regenerate-api-key?name= Regenerate current api key of device with name
    @GetMapping("/regenerate-api-key")
    public ResponseEntity<DeviceWithApiKeyModel> regenerateApiKey(
            @RequestParam String name,
            @AuthenticationPrincipal UUID userId) {

        DeviceWithApiKeyModel model = deviceService.regenerateApiKeyByName(userId, name);
        return ResponseEntity.ok(model);
    }



    // GET /api/devices/{id}/api-key Get current api key
//    @GetMapping("/{id}/api-key")
//    public ResponseEntity<String> getApiKey(
//            @PathVariable UUID id,
//            @AuthenticationPrincipal UUID userId) {
//
//        authorizationService.checkDeviceOwnership(userId, id);
//
//        String apiKey = deviceService.getApiKey(id, userId);
//        return ResponseEntity.ok(apiKey);
//    }

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