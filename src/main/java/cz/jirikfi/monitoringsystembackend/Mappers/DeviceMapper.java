package cz.jirikfi.monitoringsystembackend.Mappers;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceWithApiKeyModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeviceMapper {
    public DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
        .id(device.getId())
        .name(device.getName())
        .operatingSystem(device.getOperatingSystem())
        .ipAddress(device.getIpAddress())
        .macAddress(device.getMacAddress())
        .description(device.getDescription())
        .latitude(device.getLatitude())
        .longitude(device.getLongitude())
        .model(device.getModel())
        .sshEnabled(device.getSshEnabled())
        .lastSeen(device.getLastSeen())
        .createdAt(device.getCreatedAt())
        .updatedAt(device.getUpdatedAt())
        .ownerId(device.getOwner().getId())
        .ownerUsername(device.getOwner().getUsername())
        .pictureId(device.getPicture().getId())
        .build();
    }

    public DeviceWithApiKeyModel toModelWithApiKey(Device device) {
        return DeviceWithApiKeyModel.builder()
                .id(device.getId())
                .name(device.getName())
                .operatingSystem(device.getOperatingSystem())
                .ipAddress(device.getIpAddress())
                .macAddress(device.getMacAddress())
                .description(device.getDescription())
                .latitude(device.getLatitude())
                .longitude(device.getLongitude())
                .model(device.getModel())
                .sshEnabled(device.getSshEnabled())
                .lastSeen(device.getLastSeen())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .ownerId(device.getOwner().getId())
                .ownerUsername(device.getOwner().getUsername())
                .pictureId(device.getPicture().getId())
                .apiKey(device.getApiKey())
                .build();
    }

    public Device createToEntity(CreateDeviceModel model) {
        return Device.builder()
                .name(model.getName())
                .operatingSystem(model.getOperatingSystem())
                .ipAddress(model.getIpAddress())
                .macAddress(model.getMacAddress())
                .description(model.getDescription())
                .latitude(model.getLatitude())
                .longitude(model.getLongitude())
                .model(model.getModel())
                .sshEnabled(model.getSshEnabled() != null ? model.getSshEnabled() : false)
                .build();
    }
    public void updateEntity(Device device, UpdateDeviceModel model) {
        if (model.getName() != null) device.setName(model.getName());
        if (model.getOperatingSystem() != null) device.setOperatingSystem(model.getOperatingSystem());
        if (model.getIpAddress() != null) device.setIpAddress(model.getIpAddress());
        if (model.getMacAddress() != null) device.setMacAddress(model.getMacAddress());
        if (model.getDescription() != null) device.setDescription(model.getDescription());
        if (model.getLatitude() != null) device.setLatitude(model.getLatitude());
        if (model.getLongitude() != null) device.setLongitude(model.getLongitude());
        if (model.getModel() != null) device.setModel(model.getModel());
        if (model.getSshEnabled() != null) device.setSshEnabled(model.getSshEnabled());

        device.setUpdatedAt(Instant.now());
    }

}
