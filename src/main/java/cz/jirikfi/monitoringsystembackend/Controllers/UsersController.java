package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.CreateUserModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.GetUserModel;
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
        return ResponseEntity.status(HttpStatus.OK).body(user);
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

    // Access for users to see and work with devices
    // POST /api/users/{userId}/devices/{deviceId} grantAccessForDeviceToUser
    @PostMapping("/{userId}/devices/{deviceId}")
    public ResponseEntity grantAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        userService.grantAccess(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/users/{userId}/devices/{deviceId} revokeAccessForDeviceToUser
    @DeleteMapping("/{userId}/devices/{deviceId}")
    public ResponseEntity revokeAccessForDeviceToUser(@PathVariable UUID userId, @PathVariable UUID deviceId) {
        userService.revokeAccess(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    // POST /api/users/{userId}/devices createDevice
    @PostMapping("/{userId}/device")
    public ResponseEntity<Device> createDevice(@PathVariable UUID userId, @RequestBody @Valid CreateDeviceModel model) {
        Device device = userService.createDevice(userId, model);
        return ResponseEntity.ok().body(device);
    }

    // GET /api/users/{userId}/devices getUserDevices
    @GetMapping("/{userId}/devices")
    public ResponseEntity<List<Device>> getUserDevices(@PathVariable UUID userId) {
        List<Device> devices = userService.getUserDevices(userId);
        return ResponseEntity.ok().body(devices);
    }


    // GET /api/users?keyword=
    // Get All Users based on query ->
    // keyword - search in names and emails!
    // use: for adding users when looking for contact emails for device
    @GetMapping
    public ResponseEntity<List<GetUserModel>> getUsersByKeyword(@RequestParam String keyword) {
        List<GetUserModel> users = userService.getUsersByKeyword(keyword);

        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().body(users);
    }

}
