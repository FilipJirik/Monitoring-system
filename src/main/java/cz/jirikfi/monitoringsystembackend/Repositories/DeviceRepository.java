package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    @Query("SELECT d FROM Device d " +
            "JOIN d.userAccesses ua " +
            "WHERE ua.user.id = :userId")
    List<Device> findDevicesByUserAccess(@Param("userId") UUID userId);

    @Query("SELECT d FROM Device d " +
            "JOIN d.userAccesses ua " +
            "WHERE ua.user.id = :userId AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Device> findDevicesByUserAccessKeyword(@Param("userId") UUID userId,  @Param("keyword") String keyword);
}
