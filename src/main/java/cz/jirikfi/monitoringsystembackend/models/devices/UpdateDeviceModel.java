package cz.jirikfi.monitoringsystembackend.models.devices;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateDeviceModel {

    @Nullable
    private String name;

    @Nullable
    private String operatingSystem;

    @Nullable
    private String ipAddress;

    @Nullable
    private String macAddress;

    @Nullable
    private String description;

    @Nullable
    private Double latitude;

    @Nullable
    private Double longitude;

    @Nullable
    private String model;

    @Nullable
    private Boolean sshEnabled;
}
