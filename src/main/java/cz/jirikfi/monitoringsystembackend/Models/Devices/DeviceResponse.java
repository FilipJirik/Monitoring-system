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
    public UUID Id;
    public String Name;
    public String OperatingSystem;
    public String IpAddress;
    public String MacAddress;
    public String Description;
    public Double Latitude;
    public Double Longitude;
    public String Model;
    public Boolean SshEnabled;
    public Instant LastSeen;
    public Instant CreatedAt;
    public Instant UpdatedAt;

    public UUID OwnerId;
    public String OwnerUsername;

    public UUID PictureId;
}