package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.UserDeviceAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserDeviceAccessRepository extends JpaRepository<UserDeviceAccess, UUID> {

    UserDeviceAccess findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    // boolean existsByUserIdAndDeviceId(UUID userId, UUID deviceId);
}

