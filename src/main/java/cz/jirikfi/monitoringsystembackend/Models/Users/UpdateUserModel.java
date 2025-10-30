package cz.jirikfi.monitoringsystembackend.Models.Users;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateUserModel {
    @Nullable
    private String username;

    @Nullable
    private String password;

    @Nullable
    @Email
    private String email;
}
