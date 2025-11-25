package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Entities.UserDeviceAccess;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceInfo;
import cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess.CreatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess.UpdatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.Models.Users.CreateUserModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.UserResponse;
import cz.jirikfi.monitoringsystembackend.Models.Users.UpdateUserModel;
import cz.jirikfi.monitoringsystembackend.Services.UserService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    @Autowired
    private UserService userService;

    // CRUD
    // Create
    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody @Valid CreateUserModel model) {
        User user = userService.createUser(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    // Read
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok().body(user);
    }
    // Update
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody @Valid UpdateUserModel model) {
        User user = userService.updateUser(id, model);
        return ResponseEntity.ok().body(user);
    }
    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    // GET /api/users?keyword=
    // Get All Users based on query ->
    // keyword - search in names and emails!
    // use: for adding users when looking for contact emails for device
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsersByKeyword(@RequestParam String keyword) {
        List<UserResponse> users = userService.getUsersByKeyword(keyword);

        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().body(users);
    }

    // Access for users to see and work with devices

    // POST /api/users/{userId}/devices/{deviceId} grantAccessForDeviceToUser
    @PostMapping("/{userId}/devices/{deviceId}")
    public ResponseEntity<UserDeviceAccess> grantAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                     @RequestBody @Valid CreatePermissionRequest model) {
        UserDeviceAccess UserDeviceAccess = userService.grantAccess(userId, deviceId, model);
        return ResponseEntity.ok().body(UserDeviceAccess);
    }

    // UPDATE /api/users/{userId}/devices/{deviceId} updatePermissions
    @PutMapping("/{userId}/devices/{deviceId}")
    public ResponseEntity<UserDeviceAccess> updatePermissions(@PathVariable UUID userId, @PathVariable UUID deviceId,
                                                              @RequestBody @Valid UpdatePermissionRequest model) {
        UserDeviceAccess UserDeviceAccess = userService.changePermission(userId, deviceId, model);
        return ResponseEntity.ok().body(UserDeviceAccess);
    }

    // DELETE /api/users/{userId}/devices/{deviceId} revokeAccessForDeviceToUser
    @DeleteMapping("/{userId}/devices/{deviceId}")
    public ResponseEntity revokeAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        userService.revokeAccess(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    // Users devices

    // GET /api/users/{userId}/devices getUserDevices
//    @GetMapping("/{userId}/devices")
//    public ResponseEntity<List<Device>> getUserDevices(@PathVariable UUID userId) {
//        List<Device> devices = userService.getUserDevices(userId);
//        return ResponseEntity.ok().body(devices);
//    }

    // GET /api/users/{userId}/devices?keyword=
    @GetMapping("/{userId}/devices")
    public ResponseEntity<List<DeviceInfo>> getUserDevicesByKeyword(@PathVariable UUID userId, @RequestParam String keyword) {
        List<DeviceInfo> devices = userService.getUserDevicesByKeyword(userId, keyword);
        return ResponseEntity.ok().body(devices);
    }



}
