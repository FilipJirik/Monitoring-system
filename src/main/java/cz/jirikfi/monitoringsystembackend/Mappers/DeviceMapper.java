package cz.jirikfi.monitoringsystembackend.Mappers;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.Devices.UpdateDeviceModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeviceMapper {
    public DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
        .Id(device.getId())
        .Name(device.getName())
        .OperatingSystem(device.getOperatingSystem())
        .IpAddress(device.getIpAddress())
        .MacAddress(device.getMacAddress())
        .Description(device.getDescription())
        .Latitude(device.getLatitude())
        .Longitude(device.getLongitude())
        .Model(device.getModel())
        .SshEnabled(device.getSshEnabled())
        .LastSeen(device.getLastSeen())
        .CreatedAt(device.getCreatedAt())
        .UpdatedAt(device.getUpdatedAt())
        .OwnerId(device.getOwner().getId())
        .OwnerUsername(device.getOwner().getUsername())
        .PictureId(device.getPicture().getId())
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
