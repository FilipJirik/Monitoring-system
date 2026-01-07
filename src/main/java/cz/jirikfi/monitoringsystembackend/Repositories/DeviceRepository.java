package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    boolean existsByName(String name);

    @Query("SELECT DISTINCT d FROM Device d " +
            "LEFT JOIN d.userAccesses ua " +
            "WHERE d.owner.id = :userId OR ua.user.id = :userId")
    List<Device> findAllAccessibleByUser(@Param("userId") UUID userId);

    Optional<Device> findByOwnerAndName(User owner, String name);

//    @Query("SELECT d FROM Device d " +
//            "JOIN d.userAccesses ua " +
//            "WHERE d.owner.id = :userId OR ua.user.id = :userId AND LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
//    List<Device> findAllOwnedByName(@Param("userId") UUID userId,  @Param("keyword") String keyword);

    Device findByApiKey(String apiKey);

    List<Device> findByOwner(User owner);
}
