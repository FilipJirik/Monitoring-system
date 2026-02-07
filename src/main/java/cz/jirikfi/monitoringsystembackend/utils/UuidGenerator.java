package cz.jirikfi.monitoringsystembackend.utils;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public class UuidGenerator {
    public static UUID v7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
