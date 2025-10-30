package cz.jirikfi.monitoringsystembackend.Controllers;


import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    // For testing
//    @GetMapping("/getNames")
//    public ResponseEntity<List<String>> getAllDeviceNames() {
//        List<String> deviceNames = deviceService.getNames();
//
//        if (deviceNames == null) {
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(deviceNames);
//    }


    ///
    /// REPLACED BY 'POST /api/users/{userId}/devices' createDevice in UsersController
    ///

    // POST /api/devices
//    @PostMapping("/create")
//    public ResponseEntity<Device> createDevice(@Valid @RequestBody CreateDeviceModel model) {
//        Device device = null;
//        try{
//            device = deviceService.createDevice(model);
//
//            if (device == null){
//                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found");
//            }
//        } catch (Exception e) {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(device);
//    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /// ALL THESE ENDPOINTS NEED AUTHENTICATION THAT SENDER (USER) IS IN 'allowedUsers' ///
    ///////////////////////////////////////////////////////////////////////////////////////

    // GET /api/devices/{id} getDevice
    @GetMapping("/{id}")
    public ResponseEntity<Device> getDevice(@PathVariable UUID id) {
        Device device = deviceService.getDevice(id);
        return ResponseEntity.ok().body(device);
    }

    // PUT /api/devices/{id} updateDevice
    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable UUID id, @RequestBody @Valid UpdateDeviceModel model) {
        Device device = deviceService.updateDevice(id, model);
        return ResponseEntity.ok().body(device);
    }

    // DELETE /api/devices/{id} deleteDevice
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable UUID id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/devices/{id}/picture
    @PutMapping("/{id}/picture")
    public ResponseEntity<Device> uploadPicture(@PathVariable UUID id, @RequestParam("picture") MultipartFile picture) {
        Device device = deviceService.changePicture(id, picture);
        return ResponseEntity.ok().body(device);
    }

    // Metrics
    // GET /devices/{id}/metrics  GET all metrics of device
    @GetMapping("{id}/metrics")
    public ResponseEntity<List<Metrics>> getMetrics(@PathVariable UUID id) {
        List<Metrics> metrics = metricsService.getMetrics(id);
        return ResponseEntity.ok().body(metrics);
    }




}