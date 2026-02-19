package cz.jirikfi.monitoringsystembackend.models.recipients;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRecipientRequestDto {
    @NotNull(message = "Notify by email cannot be null")
    @Builder.Default
    private Boolean notifyEmail = true;

    @NotNull(message = "Notify by frontend cannot be null")
    @Builder.Default
    private Boolean notifyFrontend = true;
}
