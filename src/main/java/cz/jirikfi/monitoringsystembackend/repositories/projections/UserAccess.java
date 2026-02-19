package cz.jirikfi.monitoringsystembackend.repositories.projections;

import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;

import java.util.UUID;

public interface UserAccess {
    UUID getUserId();
    String getUsername();
    String getEmail();
    Boolean getIsOwner();
    PermissionLevel getPermissionLevel();
}
