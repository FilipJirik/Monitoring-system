package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertRecipientMapper {

    public RecipientResponse toResponse(AlertRecipient recipient) {
        return RecipientResponse.builder()
                .id(recipient.getId())
                .userId(recipient.getUser().getId())
                .username(recipient.getUser().getUsername())
                .email(recipient.getUser().getEmail())
                .notifyEmail(recipient.getNotifyEmail())
                .notifyFrontend(recipient.getNotifyFrontend())
                .build();
    }
}
