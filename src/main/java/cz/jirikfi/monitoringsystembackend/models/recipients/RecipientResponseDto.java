package cz.jirikfi.monitoringsystembackend.models.recipients;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipientResponseDto {

    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private Boolean notifyEmail;
    private Boolean notifyFrontend;
}
