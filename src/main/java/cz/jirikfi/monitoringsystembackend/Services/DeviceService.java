package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.PictureRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceDatabase;
    @Autowired
    private PictureRepository pictureDatabase;

    public Device getDevice(UUID id) {
        return deviceDatabase.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
    }

//    public List<String >getNames() {
//        return deviceDatabase.findAll().stream().map(Device::getName).toList();
//    }
//
//    public Device createDevice(CreateDeviceModel model) {
//        Device device = Device.builder()
//                .name(model.getName())
//                .operatingSystem(model.getOperatingSystem())
//                .ipAddress(model.getIpAddress())
//                .macAddress(model.getMacAddress())
////                .emailAddress(model.getEmailAddress())
//                .description(model.getDescription())
//                .latitude(model.getLatitude())
//                .longitude(model.getLongitude())
//                .model(model.getModel())
//                .sshEnabled(model.getSshEnabled())
////                .picture(PictureService.loadDefaultPicture())
//                .build();
//
//        deviceDatabase.save(device);
//        return device;
//    }

    public Device changePicture(UUID id, MultipartFile file) {
        Device device = getDevice(id);

        byte[] data = null;

        try {
            data = file.getBytes();
        }
        catch (IOException e) {
            throw new InternalErrorException("Could not read picture");
        }

        Picture pictureModel = Picture.builder()
                .filename(file.getOriginalFilename())
                .data(data)
                .build();

        pictureDatabase.save(pictureModel);

        device.setPicture(pictureModel);
        deviceDatabase.save(device);
        return device;
    }

    public Device updateDevice(UUID id, UpdateDeviceModel model) {
        Device device = getDevice(id);

        if (model.getName() != null) device.setName(model.getName());
        if (model.getOperatingSystem() != null) device.setOperatingSystem(model.getOperatingSystem());
        if (model.getIpAddress() != null) device.setIpAddress(model.getIpAddress());
        if (model.getMacAddress() != null) device.setMacAddress(model.getMacAddress());
        if (model.getDescription() != null) device.setDescription(model.getDescription());
        if (model.getLatitude() != null) device.setLatitude(model.getLatitude());
        if (model.getLongitude() != null) device.setLongitude(model.getLongitude());
        if (model.getModel() != null) device.setModel(model.getModel());
        if (model.getSshEnabled() != null) device.setSshEnabled(model.getSshEnabled());

        deviceDatabase.save(device);
        return device;
    }

    public void deleteDevice(UUID id) {
        Device device = getDevice(id);
        deviceDatabase.delete(device);
    }
}
