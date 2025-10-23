package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceDatabase;

    public List<Device> getAll() {
        return deviceDatabase.findAll();
    }
    public List<String >getOSs() {
        return deviceDatabase.findAll().stream().map(Device::getOperatingSystem).toList();
    }

    public Device getById(Long id) {
        return deviceDatabase.findById(id).orElse(null);
    }
    public Device save(Device device) {
        return deviceDatabase.save(device);
    }
    public Device update(Device device) {
        return deviceDatabase.save(device);
    }
    public void delete(Device device) {
        deviceDatabase.delete(device);
    }
}
