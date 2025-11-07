package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt =  Instant.now();

    @Column(name = "last_login")
    private Instant lastLogin;

    @OneToMany(mappedBy = "owner")
    @Builder.Default
    private Set<Device> ownedDevices = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<UserDeviceAccess> deviceAccesses = new HashSet<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private Set<AlertRecipient> alertRecipients = new HashSet<>();

    @OneToMany(mappedBy = "resolvedBy")
    @Builder.Default
    private Set<Alert> resolvedAlerts = new HashSet<>();
}
