package cz.jirikfi.monitoringsystembackend.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@Table(name = "alert_recipients")
@AllArgsConstructor
@NoArgsConstructor
public class AlertRecipient {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "notify_email")
    private Boolean notifyEmail = true;

    @Builder.Default
    @Column(name = "notify_frontend")
    private Boolean notifyFrontend = true;
}
