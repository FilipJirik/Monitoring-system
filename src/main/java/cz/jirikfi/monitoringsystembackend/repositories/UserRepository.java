package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.User;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u " +
            "WHERE :keyword IS NULL OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findUsersByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(value = """
        SELECT
            u.id AS userId,
            u.username AS username,
            u.email AS email,
            (CASE WHEN d.owner.id = u.id THEN true ELSE false END) AS isOwner,
            (CASE WHEN u.role = 'ADMIN' THEN true ELSE false END) AS isAdmin,
            (CASE WHEN ar.id IS NOT NULL THEN true ELSE false END) AS isRecipient,
            ar.notifyEmail AS notifyEmail,
            ar.notifyFrontend AS notifyFrontend
        FROM User u
        LEFT JOIN AlertRecipient ar ON ar.user.id = u.id AND ar.device.id = :deviceId
        LEFT JOIN u.deviceAccesses ua ON ua.device.id = :deviceId
        LEFT JOIN Device d ON d.id = :deviceId
        WHERE
        (
            ua.device.id = :deviceId
            OR d.owner.id = u.id
            OR u.role = 'ADMIN'
            OR ar.id IS NOT NULL
        )
    """,
    countQuery = """
        SELECT COUNT(u)
        FROM User u
        LEFT JOIN AlertRecipient ar ON ar.user.id = u.id AND ar.device.id = :deviceId
        LEFT JOIN u.deviceAccesses ua ON ua.device.id = :deviceId
        LEFT JOIN Device d ON d.id = :deviceId
        WHERE
        (
            ua.device.id = :deviceId
            OR d.owner.id = u.id
            OR u.role = 'ADMIN'
            OR ar.id IS NOT NULL
        )
    """)
    Page<RecipientStatus> findPotentialRecipients(@Param("deviceId") UUID deviceId, Pageable pageable);
}
