package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.PictureRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PictureRepository pictureRepository;
    @Autowired
    private PictureService pictureService;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public DeviceResponse createDevice(UUID userId, CreateDeviceModel model) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id " + userId + " not found"));

        Device device = deviceMapper.createToEntity(model);
        device.setOwner(user);

        device.setPicture(pictureService.getDefaultPicture()); // FIXME better solution

        String apiKey = GenerateApiKeyService.generate();
        device.setApiKey(passwordEncoder.encode(apiKey)); // Hash key

        deviceRepository.save(device);
        return deviceMapper.toResponse(device);
    }

    public DeviceResponse getDevice(UUID id) {
        Device device =  deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
        return deviceMapper.toResponse(device);
    }

    public DeviceResponse updateDevice(UUID id, UpdateDeviceModel model)
    {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));

        deviceMapper.updateEntity(device, model);
        device = deviceRepository.save(device);

        return deviceMapper.toResponse(device);
    }

    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
        deviceRepository.delete(device);
    }

    public DeviceResponse changePicture(UUID id, MultipartFile file) {
        Device device =  deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));

        byte[] data = null;

        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InternalErrorException("Could not read the picture");
        }

        Picture pictureModel = Picture.builder()
                .filename(file.getOriginalFilename())
                .data(data)
                .build();

        pictureRepository.save(pictureModel);

        device.setPicture(pictureModel);
        deviceRepository.save(device);

        return deviceMapper.toResponse(device);
    }

    public String regenerateApiKey(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        String newApiKey = GenerateApiKeyService.generate();
        device.setApiKey(passwordEncoder.encode(newApiKey));
        deviceRepository.save(device);

        return newApiKey;
    }
}
