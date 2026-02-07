package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.models.devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.models.devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.utils.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Dependency injection
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final DeviceMapper deviceMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyGenerator apiKeyGenerator;

    @Value("${app.pictures.default-filename:default.png}")
    private String defaultPictureFilename;

    @Transactional
    public DeviceWithApiKeyModel createDevice(UUID userId, CreateDeviceModel model) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User with id " + userId + " not found"));

        if (deviceRepository.existsByName(model.getName()))
            throw new BadRequestException("Device with name " + model.getName() + " already exists");

        Device device = deviceMapper.createToEntity(model);
        device.setOwner(user);
        device.setImageFilename(defaultPictureFilename);

        String rawApiKey = apiKeyGenerator.generate();
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
    public void changeDevicePicture(UUID id, MultipartFile file) {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));

        String oldFilename = device.getImageFilename();

        if (oldFilename != null && !oldFilename.equals(defaultPictureFilename)) {
            imageService.deleteImage(oldFilename);
        }

        String newFilename = imageService.saveImage(file);
        device.setImageFilename(newFilename);

        deviceRepository.save(device);
    }

    @Transactional
    public void resetDevicePicture(UUID id) {
        Device device = deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));

        String currentFilename = device.getImageFilename();

        if (currentFilename == null || currentFilename.equals(defaultPictureFilename)) {
            return;
        }
        imageService.deleteImage(currentFilename);

        device.setImageFilename(defaultPictureFilename);
        deviceRepository.save(device);
    }
    @Transactional
    public DeviceWithApiKeyModel regenerateApiKeyByName(UUID userId, String deviceName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Device device = deviceRepository.findByOwnerAndName(user, deviceName)
                .orElseThrow(() -> new NotFoundException(
                        "Device not found with name: " + deviceName + " (or you are not the owner)"
                ));

        String apiKey = apiKeyGenerator.generate();

        DeviceWithApiKeyModel response = deviceMapper.toModelWithApiKey(device);
        response.setApiKey(apiKey);

        device.setApiKey(passwordEncoder.encode(apiKey));

        deviceRepository.save(device);
        return response;
    }

    @Transactional
    public List<DeviceResponse> getAllAccessibleDevices(UUID userId) {
        List<Device> devices = deviceRepository.findAllAccessibleByUser(userId);

        return devices.stream().map(deviceMapper::toResponse).toList(); // FIXME ?
    }

    @Transactional
    public DeviceWithApiKeyModel getOwnedDeviceByName(UUID userId, String deviceName) { // FIXME
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
