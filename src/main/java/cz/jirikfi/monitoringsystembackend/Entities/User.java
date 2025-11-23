package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Data
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
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
        return Collections.emptyList(); // No roles
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
