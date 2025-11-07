package cz.jirikfi.monitoringsystembackend.Models.Devices;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeviceResponse {
    @NotEmpty
    private String name;
    @NotEmpty
    private Instant lastSeen;

}
