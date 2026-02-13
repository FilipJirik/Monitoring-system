package cz.jirikfi.monitoringsystembackend.repositories.projections;

import java.util.UUID;

public interface RecipientStatus {
    UUID getUserId();
    String getUsername();
    String getEmail();

    boolean getIsOwner();
    boolean getIsAdmin();

    boolean getIsRecipient();
    Boolean getNotifyEmail();
    Boolean getNotifyFrontend();
}
