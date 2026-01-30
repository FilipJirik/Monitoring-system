package cz.jirikfi.monitoringsystembackend.Models.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginModel {
    @NotBlank
    @Email
    String email;

    @NotBlank
    String password;
}
