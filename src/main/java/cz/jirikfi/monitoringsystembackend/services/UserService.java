package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserDeviceAccess;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.mappers.UserMapper;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.CreatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.DeviceAccessResponseDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UpdatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.CreateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.UserResponseDto;
import cz.jirikfi.monitoringsystembackend.models.users.UpdateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserDeviceAccessRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import Slf4j
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // Add Slf4j annotation
public class UserService {
    private final UserDeviceAccessRepository userDeviceAccessRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DeviceMapper deviceMapper;

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
                new NotFoundException("User with id " + id + " not found"));
    }
    @Transactional(readOnly = true)
    public Device getDevice(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Device with id " + id + " not found"));
    }
    @Transactional
    public User createUser(CreateUserRequestDto model) {
        if (userRepository.existsByEmail(model.getEmail())) {
            throw new ConflictException("User with email " + model.getUsername() + " already exists");
        }

        User user = User.builder()
             .username(model.getUsername())
             .email(model.getEmail())
             .password(passwordEncoder.encode(model.getPassword()))
             .build();

        userRepository.save(user);
        return user;
    }
    @Transactional
    public User updateUser(UUID id, UpdateUserRequestDto model) {
        User user = getUser(id);

        // Only check for email conflict if the email is being changed and is not null
        if (model.getEmail() != null && !user.getEmail().equals(model.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(model.getEmail(), id)) {
                throw new ConflictException("User with email " + model.getEmail() + " already exists");
            }
        }

        if (model.getUsername() != null) user.setUsername(model.getUsername());
        if (model.getEmail() != null) user.setEmail(model.getEmail());
        if (model.getPassword() != null) user.setPassword(passwordEncoder.encode(model.getPassword()));

        userRepository.save(user);

        return user;
    }
    @Transactional
    public void deleteUser(UUID id) {
        User user = getUser(id);

        if (user != null) {
            userRepository.delete(user);
        }
    }
    @Transactional
    public void grantAccess(UUID userId, UUID deviceId, CreatePermissionRequestDto model) {
        Optional<UserDeviceAccess> existingAccess = userDeviceAccessRepository.findByUserIdAndDeviceId(userId, deviceId);

        if (existingAccess.isPresent()) {
            throw new ConflictException("User " + userId + " already has access to device " + deviceId);
        }

        User user = getUser(userId);
        Device device = getDevice(deviceId);

        UserDeviceAccess userDeviceAccess = UserDeviceAccess.builder()
                .device(device)
                .user(user)
                .permissionLevel(model.getPermissionLevel())
                .build();

        userDeviceAccessRepository.save(userDeviceAccess);
    }
    @Transactional
    public void changePermission(UUID userId, UUID deviceId, UpdatePermissionRequestDto model) {
        UserDeviceAccess userDeviceAccess = userDeviceAccessRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new BadRequestException("User " + userId + " doesn't have access to device " + deviceId));

        userDeviceAccess.setPermissionLevel(model.getPermissionLevel());
        userDeviceAccessRepository.save(userDeviceAccess);
    }

    @Transactional
    public void revokeAccess(UUID userId, UUID deviceId) {
        UserDeviceAccess userDeviceAccess = userDeviceAccessRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new BadRequestException("User " + userId + " doesn't have access to device " + deviceId));

        userDeviceAccessRepository.delete(userDeviceAccess);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersByKeyword(String keyword, Pageable pageable) {
        String searchKey = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<User> usersPage;

        if (searchKey == null) {
            usersPage = userRepository.findAll(pageable);
        } else {
            usersPage = userRepository.findUsersByKeyword(searchKey, pageable);
        }
        return usersPage.map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeviceAccessResponseDto> getUserDeviceAccesses(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id" + userId + "not found");
        }

        return deviceRepository.findAccessibleDevicesForUser(userId, pageable)
                .map(deviceMapper::toDeviceAccessResponse);
    }
}
