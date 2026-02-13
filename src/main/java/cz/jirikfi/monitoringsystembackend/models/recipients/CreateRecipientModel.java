package cz.jirikfi.monitoringsystembackend.models.recipients;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateRecipientModel {
    @Nullable
    private UUID userId;

    @Builder.Default
    private Boolean notifyEmail = true;
    @Builder.Default
    private Boolean notifyFrontend = true;
}
