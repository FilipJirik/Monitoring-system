package cz.jirikfi.monitoringsystembackend.Models.Devices;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DeviceResponse {
    public UUID id;
    public String name;
    public String operatingSystem;
    public String ipAddress;
    public String macAddress;
    public String description;
    public Double latitude;
    public Double longitude;
    public String model;
    public Boolean sshEnabled;
    public Instant lastSeen;
    public Instant createdAt;
    public Instant updatedAt;

    public UUID ownerId;
    public String ownerUsername;

    public String imageFilename;
}