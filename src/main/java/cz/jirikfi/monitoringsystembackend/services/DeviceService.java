package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.models.devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.models.devices.UpdateDeviceModel;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.utils.ServerUrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final DeviceMapper deviceMapper;
    private final ServerUrlUtil serverUrlUtil;
    private final DeviceAuthService deviceAuthService;
    private final AuthorizationService authorizationService;


    @Value("${app.pictures.default-filename:default.png}")
    private String defaultPictureFilename;

    @Transactional
    public DeviceWithApiKeyModel createDevice(UserPrincipal principal, CreateDeviceModel model) {
        if (deviceRepository.existsByName(model.getName()))
            throw new ConflictException("Device with name " + model.getName() + " already exists");

        Device device = deviceMapper.createToEntity(model);
        device.setOwner(userRepository.getReferenceById(principal.getId()));
        device.setImageFilename(defaultPictureFilename);

        String rawApiKey = deviceAuthService.generateRawApiKey();
        String hashedKey = deviceAuthService.hashApiKey(rawApiKey);

        device.setApiKey(hashedKey);

        deviceRepository.save(device);

        DeviceWithApiKeyModel response = DeviceWithApiKeyModel.builder()
                .id(device.getId())
                .apiKey(rawApiKey)
                .setupCommand(serverUrlUtil.getSetupCommand(device.getId().toString(), rawApiKey))
                .build();

        response.setApiKey(rawApiKey);
        return response;
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(UserPrincipal principal, UUID id) {
        Device device = authorizationService.getDeviceWithReadAccess(id, principal);
        return deviceMapper.toResponse(device);
    }

    @Transactional
    public DeviceResponse updateDevice(UserPrincipal principal, UUID id, UpdateDeviceModel model) {
        Device device = authorizationService.getDeviceWithEditAccess(id, principal);

        deviceMapper.updateEntity(device, model);
        deviceRepository.save(device);

        return deviceMapper.toResponse(device);
    }

    @Transactional
    public void deleteDevice(UserPrincipal principal, UUID id) {
        Device device = authorizationService.getDeviceWithEditAccess(id, principal);

        tryDeleteDeviceImage(device.getImageFilename());
        deviceRepository.delete(device);
    }


    @Transactional
    public void changeDevicePicture(UserPrincipal principal, UUID deviceId, MultipartFile file) {
        Device device = authorizationService.getDeviceWithEditAccess(deviceId, principal);

        String oldFilename = device.getImageFilename();
        String newFilename = imageService.saveImage(file);

        device.setImageFilename(newFilename);
        device.setUpdatedAt(Instant.now());
        deviceRepository.save(device);

        tryDeleteDeviceImage(oldFilename);
    }


    @Transactional
    public void resetDevicePicture(UserPrincipal principal, UUID id) {
        Device device = authorizationService.getDeviceWithEditAccess(id, principal);

        String oldFilename = device.getImageFilename();
        String defaultFilename = imageService.getDefaultFilename();

        if (defaultFilename.equals(oldFilename)) {
            return;
        }

        device.setImageFilename(defaultFilename);
        device.setUpdatedAt(Instant.now());
        deviceRepository.save(device);

        imageService.deleteImage(oldFilename);
    }

    @Transactional
    public DeviceWithApiKeyModel regenerateApiKey(UserPrincipal principal, UUID id) {
        Device device = authorizationService.getDeviceWithEditAccess(id, principal);

        String rawApiKey = deviceAuthService.generateRawApiKey();
        String hashedKey = deviceAuthService.hashApiKey(rawApiKey);

        device.setApiKey(hashedKey);
        device.setUpdatedAt(Instant.now());

        deviceRepository.save(device);

        DeviceWithApiKeyModel response = DeviceWithApiKeyModel.builder()
                .id(id)
                .apiKey(rawApiKey)
                .setupCommand(serverUrlUtil.getSetupCommand(id.toString(), rawApiKey))
                .build();

        response.setApiKey(rawApiKey);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<DeviceResponse> getAllAccessibleDevices(UserPrincipal principal, String keyword, Pageable pageable) {
        String searchKey = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        boolean isAdmin = principal.getRole() == Role.ADMIN;

        return deviceRepository.searchDevices(principal.getId(), isAdmin, searchKey, pageable)
                .map(deviceMapper::toResponse);
    }

    private void tryDeleteDeviceImage(String imageFilename) {
        if (imageFilename != null && !imageFilename.equals(defaultPictureFilename)) {
            imageService.deleteImage(imageFilename);
        }
    }
}
