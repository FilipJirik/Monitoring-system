package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Models.Devices.CreateDeviceModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.CreateUserModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.GetUserModel;
import cz.jirikfi.monitoringsystembackend.Models.Users.UpdateUserModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private DeviceRepository deviceDatabase;
    @Autowired
    private UserRepository userDatabase;
    @Autowired
    private PictureService pictureService;

    public User getUser(UUID id) {
        return userDatabase.findById(id).orElseThrow(() ->
                new NotFoundException("User with id " + id + " not found"));
    }

    public Device getDevice(UUID id) {
        return deviceDatabase.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
    }

    public User createUser(CreateUserModel model) {
        User user = User.builder()
             .username(model.getUsername())
             .email(model.getEmail())
             .password(model.getPassword())
             .build();

        userDatabase.save(user);
        return user;
    }

    public User updateUser(UUID id, UpdateUserModel model) {
        User user = getUser(id);

        if (model.getUsername() != null) user.setUsername(model.getUsername());
        if (model.getEmail() != null) user.setEmail(model.getEmail());
        if (model.getPassword() != null) user.setPassword(model.getPassword());

        userDatabase.save(user);

        return user;
    }
    public void deleteUser(UUID id) {
        User user = getUser(id);
        userDatabase.delete(user);
    }

    public void grantAccess(UUID userId, UUID deviceId) {
        User user = getUser(userId);
        Device device = getDevice(deviceId);

        user.getAccessibleDevices().add(device);
    }

    public void revokeAccess(UUID userId, UUID deviceId) {
        User user = getUser(userId);
        Device device = getDevice(deviceId);

        user.getAccessibleDevices().remove(device);
    }

    public Device createDevice(UUID userId, CreateDeviceModel model) {
        User user = getUser(userId);

        Device device = Device.builder()
                .name(model.getName())
                .operatingSystem(model.getOperatingSystem())
                .ipAddress(model.getIpAddress())
                .macAddress(model.getMacAddress())
                .description(model.getDescription())
                .latitude(model.getLatitude())
                .longitude(model.getLongitude())
                .model(model.getModel())
                .sshEnabled(model.getSshEnabled())
                .allowedUsers(new HashSet<>() )
                .owner(user)
                .picture(pictureService.getDefaultPicture())
                .build();

        device.getAllowedUsers().add(user); // owner has rights to access device

        deviceDatabase.save(device);
        return device;
    }

    public List<Device> getUserDevices(UUID userId) {
        User user = getUser(userId);
        return user.getAccessibleDevices().stream().toList();
    }

    public List<GetUserModel> getUsersByKeyword(String keyword) {
        List<User> users = userDatabase.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

        return users.stream()
                .map(u -> new GetUserModel(u.getUsername(), u.getEmail()))
                .toList();
    }

}
