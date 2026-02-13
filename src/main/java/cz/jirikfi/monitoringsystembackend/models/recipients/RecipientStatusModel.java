package cz.jirikfi.monitoringsystembackend.models.recipients;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RecipientStatusModel {
    private UUID userId;
    private String username;
    private String email;

    private boolean isOwner;
    private boolean isAdmin;
    private boolean isRecipient;
    private Boolean notifyEmail;
    private Boolean notifyFrontend;
}
