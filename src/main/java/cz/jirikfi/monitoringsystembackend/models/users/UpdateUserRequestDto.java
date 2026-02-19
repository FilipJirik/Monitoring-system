package cz.jirikfi.monitoringsystembackend.models.users;

import cz.jirikfi.monitoringsystembackend.enums.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateUserRequestDto {
    @Nullable
    private String username;

    @Nullable
    @Min(value = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Nullable
    @Email(message = "Email should be valid")
    private String email;

    @Nullable
    private Role role;
}
