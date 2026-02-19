package cz.jirikfi.monitoringsystembackend.models.devices;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import cz.jirikfi.monitoringsystembackend.validation.IpAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateDeviceRequestDto {

    @Nullable
    private String name;

    @Nullable
    private String operatingSystem;

    @Nullable
    @IpAddress(message = "Invalid IP address format")
    private String ipAddress;

    @Nullable
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "Invalid MAC address format")
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
