package cz.jirikfi.monitoringsystembackend.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@Table(name = "user_device_access")
@IdClass(UserDeviceAccessId.class)
@AllArgsConstructor
@NoArgsConstructor
public class UserDeviceAccess {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 4)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.READ;
}



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class UserDeviceAccessId implements java.io.Serializable {
    private UUID user;
    private UUID device;
}