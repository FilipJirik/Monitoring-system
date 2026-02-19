package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.models.devices.CreateDeviceRequestDto;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceResponseDto;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceWithApiKeyDto;
import cz.jirikfi.monitoringsystembackend.models.devices.UpdateDeviceRequestDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UserAccessResponseDto;
import cz.jirikfi.monitoringsystembackend.services.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DevicesController {
    private final DeviceService deviceService;

    private final org.slf4j.Logger _logger = org.slf4j.LoggerFactory.getLogger(DevicesController.class);
    // DEVICES

    // POST /api/devices createDevice
    @PostMapping
    public ResponseEntity<DeviceWithApiKeyDto> createDevice(
            @RequestBody @Valid CreateDeviceRequestDto model,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceWithApiKeyDto device = deviceService.createDevice(principal, model);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    // PUT /api/devices/{id} updateDevice
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> updateDevice(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDeviceRequestDto model,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponseDto device = deviceService.updateDevice(principal, id, model);
        return ResponseEntity.ok().body(device);
    }

    // DELETE /api/devices/{id} deleteDevice
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceService.deleteDevice(principal, id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/devices/{id} GET device by id
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDevice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponseDto device = deviceService.getDevice(principal, id);
        return ResponseEntity.ok().body(device);
    }

    // GET /api/devices GET all devices
    @GetMapping
    public ResponseEntity<Page<DeviceResponseDto>> getAllDevices(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DeviceResponseDto> devices = deviceService.getAllAccessibleDevices(principal, keyword, pageable);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserAccessResponseDto>> getDeviceUsers(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(deviceService.getDeviceUsers(id, pageable));
    }

    // PICTURES

    // POST /api/devices/{id}/image upload picture
    @PostMapping("/{id}/image")
    public ResponseEntity<Void> uploadPicture(
            @PathVariable UUID id,
            @Valid @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceService.changeDevicePicture(principal, id, file);
        return ResponseEntity.noContent().build();
    }
    // DELETE /api/devices/{id}/image delete picture = set default picture
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deletePicture(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceService.resetDevicePicture(principal, id);
        return ResponseEntity.noContent().build();
    }

    // API KEYS

    // POST /api/devices/{id}/regenerate-api-key Regenerate current api key of device
    @PostMapping("/{id}/regenerate-api-key")
    public ResponseEntity<DeviceWithApiKeyDto> regenerateApiKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceWithApiKeyDto model = deviceService.regenerateApiKey(principal, id);
        return ResponseEntity.ok(model);
    }
}