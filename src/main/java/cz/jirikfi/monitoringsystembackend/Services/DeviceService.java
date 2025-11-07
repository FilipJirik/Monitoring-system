package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import cz.jirikfi.monitoringsystembackend.Exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.PictureRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private PictureRepository pictureRepository;
    @Autowired
    private UserRepository userRepository;

    public Device getDevice(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() ->
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

        pictureRepository.save(pictureModel);

        device.setPicture(pictureModel);
        deviceRepository.save(device);
        return device;
    }

    public Device createDevice(CreateDeviceModel model) {

        if (!userRepository.existsById(model.getUserId())) {
            throw new NotFoundException("User with id " + model.getUserId() + " not found");
        }

        Device device = new Device();
        SetValuesIfNotNull(device, model.getName(), model.getOperatingSystem(),
                model.getIpAddress(), model.getMacAddress(), model.getDescription(),
                model.getLatitude(), model.getLongitude(), model.getModel(),
                model.getSshEnabled());

        deviceRepository.save(device);
        return device;
    } // TODO: GENERATE API KEY


    public Device updateDevice(UUID id, UpdateDeviceModel model)
    {
        Device device = getDevice(id);

        SetValuesIfNotNull(device, model.getName(), model.getOperatingSystem(),
                model.getIpAddress(), model.getMacAddress(), model.getDescription(),
                model.getLatitude(), model.getLongitude(), model.getModel(),
                model.getSshEnabled());

        deviceRepository.save(device);
        return device;
    }

    public void deleteDevice(UUID id) {
        Device device = getDevice(id);
        deviceRepository.delete(device);
    }

    private void SetValuesIfNotNull(Device device, String name, String operatingSystem, String ipAddress, String macAddress, String description, Double latitude, Double longitude, String model2, Boolean sshEnabled) {
        if (name != null) device.setName(name);
        if (operatingSystem != null) device.setOperatingSystem(operatingSystem);
        if (ipAddress != null) device.setIpAddress(ipAddress);
        if (macAddress != null) device.setMacAddress(macAddress);
        if (description != null) device.setDescription(description);
        if (latitude != null) device.setLatitude(latitude);
        if (longitude != null) device.setLongitude(longitude);
        if (model2 != null) device.setModel(model2);
        if (sshEnabled != null) device.setSshEnabled(sshEnabled);
    }
}
