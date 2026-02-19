package cz.jirikfi.monitoringsystembackend.models.users;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserResponseDto {

    public UUID id;

    public String username;

    public String email;
}

