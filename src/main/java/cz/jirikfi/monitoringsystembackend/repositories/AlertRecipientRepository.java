package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.repositories.projections.RecipientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRecipientRepository extends JpaRepository<AlertRecipient, UUID> {

    @Query(value = """
        SELECT
            u.id AS userId,
            u.username AS username,
            u.email AS email,
            (CASE WHEN d.owner.id = u.id THEN true ELSE false END) AS isOwner,
            (CASE WHEN u.role = 'ADMIN' THEN true ELSE false END) AS isAdmin,
            true AS isRecipient,
            ar.notifyEmail AS notifyEmail,
            ar.notifyFrontend AS notifyFrontend
        FROM AlertRecipient ar
        JOIN ar.user u
        JOIN ar.device d
        WHERE ar.device.id = :deviceId
    """,
    countQuery = """
        SELECT COUNT(ar)
        FROM AlertRecipient ar
        WHERE ar.device.id = :deviceId
    """)
    Page<RecipientStatus> findActiveRecipientsProjected(@Param("deviceId") UUID deviceId, Pageable pageable);

    Optional<AlertRecipient> findByDeviceIdAndUserId(UUID deviceId, UUID userId);

    boolean existsByDeviceIdAndUserId(UUID deviceId, UUID targetUserId);

    @Query("""
        SELECT ar
        FROM AlertRecipient ar
        JOIN FETCH ar.user
        WHERE ar.device.id = :deviceId
    """)
    List<AlertRecipient> findByDeviceIdWithUser(@Param("deviceId") UUID deviceId);

    boolean existsByUserIdAndDeviceId(UUID userId, UUID deviceId);
}
