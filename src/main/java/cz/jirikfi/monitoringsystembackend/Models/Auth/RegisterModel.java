package cz.jirikfi.monitoringsystembackend.Models.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterModel {
    @NotBlank
    @Size(min = 3, max = 50)
    String username;

    @NotBlank()
    @Size(min = 8)
    String password;

    @NotBlank
    @Email
    String email;
}
