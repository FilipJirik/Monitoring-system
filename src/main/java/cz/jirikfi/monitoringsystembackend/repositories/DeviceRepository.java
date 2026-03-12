package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.repositories.projections.DeviceAccess;
import cz.jirikfi.monitoringsystembackend.repositories.projections.DeviceAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    boolean existsByName(String name);

    @Query(value = """
        SELECT DISTINCT d FROM Device d
        LEFT JOIN FETCH d.owner
        LEFT JOIN d.userAccesses ua
        WHERE(
            (:keyword IS NULL OR :keyword = '') OR
            (
                LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(d.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(d.macAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        ) AND (
            :isGlobalAdmin = true OR
            d.owner.id = :userId OR
            ua.user.id = :userId
        )
        ORDER BY d.lastSeen DESC NULLS LAST, d.name ASC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT d) FROM Device d
        LEFT JOIN d.userAccesses ua
        WHERE(
            (:keyword IS NULL OR :keyword = '') OR
            (
                LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(d.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(d.macAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        ) AND (
            :isGlobalAdmin = true OR
            d.owner.id = :userId OR
            ua.user.id = :userId
        )
    """)
    Page<Device> searchDevices(
            @Param("userId") UUID userId,
            @Param("isGlobalAdmin") boolean isGlobalAdmin,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
        SELECT COUNT(d) > 0 FROM Device d
        LEFT JOIN d.userAccesses ua
        WHERE d.id = :deviceId
          AND (d.owner.id = :userId OR ua.user.id = :userId)
    """)
    boolean hasReadAccess(@Param("deviceId") UUID deviceId, @Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(d) > 0 FROM Device d
        LEFT JOIN d.userAccesses ua
        WHERE d.id = :deviceId
          AND (d.owner.id = :userId OR (ua.user.id = :userId AND ua.permissionLevel = 'EDIT'))
    """)
    boolean hasEditAccess(@Param("deviceId") UUID deviceId, @Param("userId") UUID userId);


    @Query("""
        SELECT d FROM Device d
        LEFT JOIN FETCH d.owner
        LEFT JOIN d.userAccesses ua
        WHERE d.id = :deviceId
          AND (d.owner.id = :userId OR ua.user.id = :userId)
    """)
    Optional<Device> findByIdAndUserAccess(@Param("deviceId") UUID deviceId,
                                           @Param("userId") UUID userId);

    @Query("""
        SELECT d FROM Device d
        LEFT JOIN FETCH d.owner
        LEFT JOIN d.userAccesses ua
        WHERE d.id = :deviceId
          AND (d.owner.id = :userId OR (ua.user.id = :userId AND ua.permissionLevel = 'EDIT'))
    """)
    Optional<Device> findByIdAndUserEditAccess(@Param("deviceId") UUID deviceId,
                                               @Param("userId") UUID userId);

    @Query("SELECT d.id as id, d.name as name, d.apiKey as apiKey, d.lastSeen as lastSeen " +
            "FROM Device d WHERE d.id = :id AND d.apiKey = :apiKey")
    Optional<DeviceAuth> findByIdAndApiKey(@Param("id") UUID id, @Param("apiKey") String apiKey);


    @Modifying
    @Query("UPDATE Device d SET d.lastSeen = :lastSeen WHERE d.id = :id")
    void updateLastSeen(@Param("id") UUID id, @Param("lastSeen") Instant lastSeen);

    @Query(value = """
        SELECT
            d.id AS deviceId,
            d.name AS deviceName,
            (CASE WHEN d.owner.id = :userId THEN true ELSE false END) AS isOwner,
            ua.permissionLevel AS permissionLevel
        FROM Device d
        LEFT JOIN d.userAccesses ua ON ua.user.id = :userId
        WHERE d.owner.id = :userId OR ua.user.id = :userId
    """,
    countQuery = """
        SELECT COUNT(d) FROM Device d
        LEFT JOIN d.userAccesses ua ON ua.user.id = :userId
        WHERE d.owner.id = :userId OR ua.user.id = :userId
    """)
    Page<DeviceAccess> findAccessibleDevicesForUser(@Param("userId") UUID userId, Pageable pageable);
}
