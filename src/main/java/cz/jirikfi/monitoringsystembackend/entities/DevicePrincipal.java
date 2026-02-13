package cz.jirikfi.monitoringsystembackend.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DevicePrincipal implements Principal {
    private final UUID deviceId;
    private final String name;

    @Override
    public String getName() {
        return name;
    }
}