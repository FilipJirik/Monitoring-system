package cz.jirikfi.monitoringsystembackend.Entities;


import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "user_device_access")
@IdClass(UserDeviceAccessId.class)
@AllArgsConstructor
@NoArgsConstructor
public class UserDeviceAccess {
    @Id
    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne()
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 4)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.READ;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class UserDeviceAccessId implements java.io.Serializable {
    private UUID user;
    private UUID device;
}