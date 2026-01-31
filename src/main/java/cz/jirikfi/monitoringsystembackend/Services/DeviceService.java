package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
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

        String apiKey = GenerateApiKeyService.generate();

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
