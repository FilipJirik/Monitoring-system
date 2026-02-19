package cz.jirikfi.monitoringsystembackend.models.devices;

import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeviceWithApiKeyDto {
    public UUID id;
    public String apiKey;
    public String setupCommand;
}
