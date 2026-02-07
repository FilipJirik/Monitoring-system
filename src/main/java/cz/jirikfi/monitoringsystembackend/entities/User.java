package cz.jirikfi.monitoringsystembackend.entities;

import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import jakarta.persistence.*;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Setter
@Getter
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
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
    private Set<Device> ownedDevices = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Builder.Default
    private Set<UserDeviceAccess> deviceAccesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Builder.Default
    private Set<AlertRecipient> alertRecipients = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "resolvedBy")
    @Builder.Default
    private Set<Alert> resolvedAlerts = new HashSet<>();


    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // UserDetails - neccessary
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
