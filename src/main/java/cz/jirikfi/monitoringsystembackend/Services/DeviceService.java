package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.PictureRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Dependency injection
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final PictureRepository pictureRepository;
    private final PictureService pictureService;
    private final DeviceMapper deviceMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public DeviceWithApiKeyModel createDevice(UUID userId, CreateDeviceModel model) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id " + userId + " not found"));

        if (deviceRepository.existsByName(model.getName()))
            throw new BadRequestException("Device with name " + model.getName() + " already exists");

        Device device = deviceMapper.createToEntity(model);
        device.setOwner(user);

        device.setPicture(pictureService.getDefaultPicture()); // FIXME better solution

        String rawApiKey = GenerateApiKeyService.generate();
        device.setApiKey(passwordEncoder.encode(rawApiKey));

        DeviceWithApiKeyModel response = deviceMapper.toModelWithApiKey(device);
        response.setApiKey(rawApiKey);

        deviceRepository.save(device);
        return response;
    }
    @Transactional(readOnly = true)
    public DeviceResponse getDevice(UUID id) {
        Device device =  deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
        return deviceMapper.toResponse(device);
    }

    @Transactional
    public DeviceResponse updateDevice(UUID id, UpdateDeviceModel model)
    {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));

        deviceMapper.updateEntity(device, model);
        device = deviceRepository.save(device);

        return deviceMapper.toResponse(device);
    }
    @Transactional
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
        deviceRepository.delete(device);
    }
    @Transactional
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
    @Transactional
    public String regenerateApiKey(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        String newApiKey = GenerateApiKeyService.generate();
        device.setApiKey(passwordEncoder.encode(newApiKey));
        deviceRepository.save(device);

        return newApiKey;
    }
    @Transactional
    public DeviceWithApiKeyModel regenerateApiKeyByName(UUID userId, String deviceName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findByOwnerAndName(user, deviceName)
                .orElseThrow(() -> new NotFoundException(
                        "Device not found with name: " + deviceName + " (or you are not the owner)"
                ));

        String apiKey = GenerateApiKeyService.generate();

        DeviceWithApiKeyModel response = deviceMapper.toModelWithApiKey(device);
        response.setApiKey(apiKey);

        device.setApiKey(passwordEncoder.encode(apiKey));

        deviceRepository.save(device);
        return response;
    }

//    public String getApiKey(UUID deviceId, UUID userId) {
//        Device device = deviceRepository.findById(deviceId)
//                .orElseThrow(() -> new NotFoundException("Device not found"));
//
//        return device.getApiKey();
//    } // FIXME - I have Api key hashed so cannot return it
    @Transactional
    public List<DeviceResponse> getAllAccessibleDevices(UUID userId) {
        List<Device> devices = deviceRepository.findAllAccessibleByUser(userId);

        return devices.stream().map(deviceMapper::toResponse).toList(); // FIXME ?
    }

    @Transactional
    public DeviceWithApiKeyModel getOwnedDeviceByName(UUID userId, String deviceName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findByOwnerAndName(user, deviceName)
                .orElseThrow(() -> new NotFoundException(
                        "Device not found with name: " + deviceName + " (or you are not the owner)"
                ));

        return deviceMapper.toModelWithApiKey(device);
    }
    @Transactional
    public List<DeviceResponse> getOwnedDevices(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<Device> devices = deviceRepository.findByOwner(user);

        return devices.stream()
                .map(deviceMapper::toResponse).toList();
    }


}
