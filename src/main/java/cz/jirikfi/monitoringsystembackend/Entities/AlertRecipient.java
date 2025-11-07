package cz.jirikfi.monitoringsystembackend.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "alert_recipients")
@AllArgsConstructor
@NoArgsConstructor
public class AlertRecipient {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne()
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "notify_email")
    private Boolean notifyEmail = true;

    @Builder.Default
    @Column(name = "notify_frontend")
    private Boolean notifyFrontend = true;
}
