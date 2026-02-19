package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.CreatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.DeviceAccessResponseDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UpdatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.CreateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.UserResponseDto;
import cz.jirikfi.monitoringsystembackend.models.users.UpdateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {
    private final UserService userService;

    // USERS

    // Create
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> addUser(@RequestBody @Valid CreateUserRequestDto model) {
        User user = userService.createUser(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    // Read
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok().body(user);
    }
    // Update
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody @Valid UpdateUserRequestDto model) {
        User user = userService.updateUser(id, model);
        return ResponseEntity.ok().body(user);
    }
    // Delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    // GET /api/users?keyword=
    // Get All Users based on query ->
    // keyword - search in names and emails!
    // use: for adding users when looking for contact emails for device
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getUsersByKeyword(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<UserResponseDto> users = userService.getUsersByKeyword(keyword, pageable);
        return ResponseEntity.ok().body(users);
    }

    // Access for users to see and work with devices

    // POST /api/users/{userId}/devices/{deviceId} grantAccessForDeviceToUser
    @PostMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> grantAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                                            @RequestBody @Valid CreatePermissionRequestDto model) {
        userService.grantAccess(userId, deviceId, model);
        return ResponseEntity.noContent().build();
    }

    // UPDATE /api/users/{userId}/devices/{deviceId} updatePermissions
    @PutMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updatePermissions(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                              @RequestBody @Valid UpdatePermissionRequestDto model) {
        userService.changePermission(userId, deviceId, model);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/users/{userId}/devices/{deviceId} revokeAccessForDeviceToUser
    @DeleteMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        userService.revokeAccess(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/users/{userId}/devices
    @GetMapping("/{userId}/devices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DeviceAccessResponseDto>> getUserDeviceAccesses(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(userService.getUserDeviceAccesses(userId, pageable));
    }
}
