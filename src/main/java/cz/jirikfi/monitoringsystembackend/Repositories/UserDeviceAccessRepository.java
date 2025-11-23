package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.Entities.UserDeviceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserDeviceAccessRepository extends JpaRepository<UserDeviceAccess, UUID> {

    UserDeviceAccess findByUserIdAndDeviceId(UUID userId, UUID deviceId);

    @Query("SELECT uda.permissionLevel FROM UserDeviceAccess uda " +
            "WHERE uda.user.id = :userId AND uda.device.id = :deviceId")

    PermissionLevel findPermissionLevel(@Param("userId") UUID userId,
                                                  @Param("deviceId") UUID deviceId);

    boolean existsByUserIdAndDeviceId(UUID userId, UUID deviceId);


}

