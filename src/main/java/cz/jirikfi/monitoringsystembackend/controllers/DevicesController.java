package cz.jirikfi.monitoringsystembackend.controllers;


import cz.jirikfi.monitoringsystembackend.services.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/// Origin for accessing information about stored devices

@RestController
@RequestMapping("/api/devices")
public class DevicesController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/getOSs")
    public List<String> getAllDeviceNames() {
        return deviceService.getOSs();
    }
}