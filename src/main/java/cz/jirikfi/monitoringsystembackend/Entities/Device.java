package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "devices")
@AllArgsConstructor
@NoArgsConstructor
public class Device {
    @Id
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @Column(nullable = false)
    private String name;

    @Column(name = "operating_system")
    private String operatingSystem;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "mac_address")
    private String macAddress;

    private String description;

    private Long latitude;

    private Long longitude;

    private String model;

    @Column(name = "ssh_enabled")
    private Boolean sshEnabled;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "picture_id")
    private Picture picture;

    @ManyToMany(mappedBy = "accessibleDevices")
    private Set<User> allowedUsers = new HashSet<>();
}
