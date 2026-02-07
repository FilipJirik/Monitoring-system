package cz.jirikfi.monitoringsystembackend.models.devices;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

// DTO
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateDeviceModel {
    @NotEmpty
    private String name;

    @NotNull
    private String operatingSystem;

    @NotNull
    private String ipAddress;

    @Nullable
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
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

