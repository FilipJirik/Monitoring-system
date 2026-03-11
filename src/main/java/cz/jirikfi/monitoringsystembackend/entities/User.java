package cz.jirikfi.monitoringsystembackend.entities;

import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import jakarta.persistence.*;

import lombok.*;

import java.time.Instant;
import java.util.*;

@Setter
@Getter
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Builder.Default
    private UUID id = UuidGenerator.v7();

    @Column(nullable = false)
    private String username;

    @Column(name="password_hash", nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt =  Instant.now();

    @Column(name = "last_login")
    private Instant lastLogin;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner")
    @Builder.Default
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private Set<Device> ownedDevices = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserDeviceAccess> deviceAccesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AlertRecipient> alertRecipients = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "resolvedBy")
    @Builder.Default
    private Set<Alert> resolvedAlerts = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RefreshToken> refreshTokens = new HashSet<>();
}
