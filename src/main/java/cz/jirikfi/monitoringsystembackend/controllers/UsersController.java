package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserDeviceAccess;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.CreatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UpdatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.models.users.CreateUserModel;
import cz.jirikfi.monitoringsystembackend.models.users.UserResponse;
import cz.jirikfi.monitoringsystembackend.models.users.UpdateUserModel;
import cz.jirikfi.monitoringsystembackend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<User> addUser(@RequestBody @Valid CreateUserModel model) {
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
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody @Valid UpdateUserModel model) { // FIXME: Add logic for changing role
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
    public ResponseEntity<List<UserResponse>> getUsersByKeyword(@RequestParam(required = false) String keyword) {

        List<UserResponse> users = userService.getUsersByKeyword(keyword);
        return ResponseEntity.ok().body(users);
    }

    // Access for users to see and work with devices

    // POST /api/users/{userId}/devices/{deviceId} grantAccessForDeviceToUser
    @PostMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDeviceAccess> grantAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                     @RequestBody @Valid CreatePermissionRequest model) {
        UserDeviceAccess UserDeviceAccess = userService.grantAccess(userId, deviceId, model);
        return ResponseEntity.ok().body(UserDeviceAccess);
    }

    // UPDATE /api/users/{userId}/devices/{deviceId} updatePermissions
    @PutMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDeviceAccess> updatePermissions(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                              @RequestBody @Valid UpdatePermissionRequest model) {
        UserDeviceAccess UserDeviceAccess = userService.changePermission(userId, deviceId, model);
        return ResponseEntity.ok().body(UserDeviceAccess);
    }

    // DELETE /api/users/{userId}/devices/{deviceId} revokeAccessForDeviceToUser
    @DeleteMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        userService.revokeAccess(userId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
