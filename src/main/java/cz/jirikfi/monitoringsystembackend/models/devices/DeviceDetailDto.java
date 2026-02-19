package cz.jirikfi.monitoringsystembackend.models.devices;

import cz.jirikfi.monitoringsystembackend.entities.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDetailDto {
    @NotNull
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    private String operatingSystem;

    @NotNull
    private String ipAddress;

    @NotNull
    private String macAddress;

    @NotNull
    private String description;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    private String model;

    @NotNull
    private Boolean sshEnabled;

    @NotNull
    private Instant lastSeen;

    @NotNull
    private Instant updatedAt;

    @NotNull
    private User owner;

    @NotNull
    private String imageFilename;
}