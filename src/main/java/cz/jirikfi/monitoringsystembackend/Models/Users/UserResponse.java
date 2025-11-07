package cz.jirikfi.monitoringsystembackend.Models.Users;

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
public class UserResponse {
    @NotEmpty
    public String username;

    @Email
    public String email;
}

