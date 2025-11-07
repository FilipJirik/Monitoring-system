package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Entities.UserDeviceAccess;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Models.Devices.DeviceResponse;
import cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess.CreatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess.UpdatePermissionRequest;
import cz.jirikfi.monitoringsystembackend.Models.Users.CreateUserModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.UserResponse;
import cz.jirikfi.monitoringsystembackend.Models.Users.UpdateUserModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserDeviceAccessRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserDeviceAccessRepository userDeviceAccessRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DeviceRepository deviceRepository;

    public User getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
                new NotFoundException("User with id " + id + " not found"));
    }

    public Device getDevice(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
    }

    public User createUser(CreateUserModel model) {
        User user = User.builder()
             .username(model.getUsername())
             .email(model.getEmail())
             .password(model.getPassword())
             .build();

        userRepository.save(user);
        return user;
    }

    public User updateUser(UUID id, UpdateUserModel model) {
        User user = getUser(id);

        if (model.getUsername() != null) user.setUsername(model.getUsername());
        if (model.getEmail() != null) user.setEmail(model.getEmail());
        if (model.getPassword() != null) user.setPassword(model.getPassword());

        userRepository.save(user);

        return user;
    }
    public void deleteUser(UUID id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    public UserDeviceAccess grantAccess(UUID userId, UUID deviceId, CreatePermissionRequest model) {
        User user = getUser(userId);
        Device device = getDevice(deviceId);

        UserDeviceAccess userDeviceAccess = UserDeviceAccess.builder()
                .device(device)
                .user(user)
                .permissionLevel(model.getPermissionLevel())
                .build();

        userDeviceAccessRepository.save(userDeviceAccess);
        return userDeviceAccess;
    }

    public void revokeAccess(UUID userId, UUID deviceId) {
        UserDeviceAccess userDeviceAccess = userDeviceAccessRepository.findByUserIdAndDeviceId(userId, deviceId);

        if (userDeviceAccess == null){
            throw new BadRequestException("User " + userId + " doesn't have access to device " + deviceId);
        }
        userDeviceAccessRepository.delete(userDeviceAccess);
    }

    public UserDeviceAccess changePermission(UUID userId, UUID deviceId, UpdatePermissionRequest model) {
        UserDeviceAccess userDeviceAccess = userDeviceAccessRepository.findByUserIdAndDeviceId(userId, deviceId);

        if (userDeviceAccess == null){
            throw new BadRequestException("User " + userId + " doesn't have access to device " + deviceId);
        }
        userDeviceAccess.setPermissionLevel(model.getPermissionLevel());
        userDeviceAccessRepository.save(userDeviceAccess);

        return userDeviceAccess;
    }

//    public Device createDevice(UUID userId, CreateDeviceModel model) {
//        User user = getUser(userId);
//
//        Device device = Device.builder()
//                .name(model.getName())
//                .operatingSystem(model.getOperatingSystem())
//                .ipAddress(model.getIpAddress())
//                .macAddress(model.getMacAddress())
//                .description(model.getDescription())
//                .latitude(model.getLatitude())
//                .longitude(model.getLongitude())
//                .model(model.getModel())
//                .sshEnabled(model.getSshEnabled())
//                .allowedUsers(new HashSet<>() )
//                .owner(user)
//                .picture(pictureService.getDefaultPicture())
//                .build();
//
//        device.getAllowedUsers().add(user); // owner has rights to access device
//
//        deviceDatabase.save(device);
//        return device;
//    }

    public List<Device> getUserDevices(UUID userId) {
        List<Device> devices = deviceRepository.findDevicesByUserAccess(userId);
        return devices;
    }
    public List<DeviceResponse> getUserDevicesByKeyword(UUID userId, String keyword) {
        List<Device> devices = deviceRepository.findDevicesByUserAccessKeyword(userId, keyword);

        return devices.stream()
                .map(d -> new DeviceResponse(d.getName(), d.getLastSeen()))
                .toList();
    }

    public List<UserResponse> getUsersByKeyword(String keyword) {
        List<User> users = userRepository.findUsersByKeyword(keyword);

        return users.stream()
                .map(u -> new UserResponse(u.getUsername(), u.getEmail()))
                .toList();
    }

}
