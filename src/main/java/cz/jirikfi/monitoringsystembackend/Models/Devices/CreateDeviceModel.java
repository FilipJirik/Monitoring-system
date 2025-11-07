package cz.jirikfi.monitoringsystembackend.Models.Devices;


import cz.jirikfi.monitoringsystembackend.Entities.User;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.util.List;
import java.util.Set;
import java.util.UUID;

// DTO
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateDeviceModel {
    @NotNull
    private UUID userId;

    @NotEmpty
    private String name;

    @NotNull
    private String operatingSystem;

    @NotNull
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
    private Boolean sshEnabled = false;
}

