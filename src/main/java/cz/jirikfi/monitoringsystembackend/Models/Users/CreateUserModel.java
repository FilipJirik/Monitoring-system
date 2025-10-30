package cz.jirikfi.monitoringsystembackend.Models.Users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;


// DTO
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateUserModel {
    @NotEmpty
    private String username;

    @NotNull
    private String password;

    @Email
    private String email;
}
