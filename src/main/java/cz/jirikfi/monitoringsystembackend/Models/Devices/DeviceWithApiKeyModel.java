package cz.jirikfi.monitoringsystembackend.Models.Devices;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeviceWithApiKeyModel {
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

    public String apiKey;
}
