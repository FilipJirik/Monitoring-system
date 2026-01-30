package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;

import lombok.*;

import java.time.Instant;
import java.util.*;

@Setter
@Getter
@Entity
@Builder
@Table(name = "devices")
@AllArgsConstructor
@NoArgsConstructor
public class Device {
    @Id
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    private String description;

    private Double latitude;

    private Double longitude;

    private String model;

    @Builder.Default
    @Column(name = "ssh_enabled")
    private Boolean sshEnabled = false;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "api_key", unique = true, nullable = false)
    private String apiKey;

    @Builder.Default
    @Column(name = "image_filename")
    private String imageFilename = "default.png";

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UserDeviceAccess> userAccesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Metrics> metrics = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Alert> alerts = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AlertThreshold> alertThresholds = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AlertRecipient> alertRecipients = new ArrayList<>();
}
