package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseModel;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusModel;
import cz.jirikfi.monitoringsystembackend.repositories.projections.RecipientStatus;
import org.springframework.stereotype.Component;

@Component
public class AlertRecipientMapper {

    public RecipientResponseModel toResponse(AlertRecipient recipient) {
        return RecipientResponseModel.builder()
                .id(recipient.getId())
                .userId(recipient.getUser().getId())
                .username(recipient.getUser().getUsername())
                .email(recipient.getUser().getEmail())
                .notifyEmail(recipient.getNotifyEmail())
                .notifyFrontend(recipient.getNotifyFrontend())
                .build();
    }

    public RecipientStatusModel mapProjectionToModel(RecipientStatus p) {
        return new RecipientStatusModel(
                p.getUserId(),
                p.getUsername(),
                p.getEmail(),
                p.getIsOwner(),
                p.getIsAdmin(),
                p.getIsRecipient(),
                p.getNotifyEmail(),
                p.getNotifyFrontend()
        );
    }
}
