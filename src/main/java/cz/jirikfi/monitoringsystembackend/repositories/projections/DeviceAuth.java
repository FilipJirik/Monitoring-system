package cz.jirikfi.monitoringsystembackend.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface DeviceAuth {
    UUID getId();
    String getApiKeyHash();
    String getName();
    Instant getLastSeen();
}

