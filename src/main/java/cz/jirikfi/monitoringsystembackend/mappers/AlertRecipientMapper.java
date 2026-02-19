package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusDto;
import cz.jirikfi.monitoringsystembackend.repositories.projections.RecipientStatus;
import org.springframework.stereotype.Component;

@Component
public class AlertRecipientMapper {

    public RecipientResponseDto toResponse(AlertRecipient recipient) {
        return RecipientResponseDto.builder()
                .id(recipient.getId())
                .userId(recipient.getUser().getId())
                .username(recipient.getUser().getUsername())
                .email(recipient.getUser().getEmail())
                .notifyEmail(recipient.getNotifyEmail())
                .notifyFrontend(recipient.getNotifyFrontend())
                .build();
    }

    public RecipientStatusDto mapProjectionToModel(RecipientStatus p) {
        return new RecipientStatusDto(
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
