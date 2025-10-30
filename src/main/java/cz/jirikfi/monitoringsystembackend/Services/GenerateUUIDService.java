package cz.jirikfi.monitoringsystembackend.Services;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

public class GenerateUUIDService {
    public static UUID v7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
