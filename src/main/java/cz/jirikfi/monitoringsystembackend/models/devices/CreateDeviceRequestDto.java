package cz.jirikfi.monitoringsystembackend.models.devices;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import cz.jirikfi.monitoringsystembackend.validation.IpAddress;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateDeviceRequestDto {
    @NotEmpty(message = "Device name cannot be empty")
    private String name;

    @NotNull(message = "Operating system cannot be null")
    private String operatingSystem;

    @NotNull(message = "IP address cannot be null")
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
    @Builder.Default
    private Boolean sshEnabled = false;
}
