package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.entities.UserDeviceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserDeviceAccessRepository extends JpaRepository<UserDeviceAccess, UUID> {

    Optional<UserDeviceAccess> findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    @Query("SELECT uda.permissionLevel FROM UserDeviceAccess uda " +
            "WHERE uda.user.id = :userId AND uda.device.id = :deviceId")

    PermissionLevel findPermissionLevel(@Param("userId") UUID userId,
                                                  @Param("deviceId") UUID deviceId);
}

