package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.*;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertThresholdMapper;
import cz.jirikfi.monitoringsystembackend.models.thresholds.CreateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.ThresholdResponseDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.UpdateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertThresholdRepository;
import cz.jirikfi.monitoringsystembackend.utils.MetricUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertThresholdService {

    private final AlertThresholdRepository alertThresholdRepository;
    private final ThresholdCacheService thresholdCacheService;
    private final AlertThresholdMapper alertThresholdMapper;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<ThresholdResponseDto> getThresholdsByDeviceId(UserPrincipal principal, UUID deviceId) {
        // Lightweight check - we don't need the Device entity, just working with thresholds
        authorizationService.verifyReadAccess(deviceId, principal);

        return alertThresholdRepository.findByDevice_IdOrderByMetricType(deviceId).stream()
                .map(alertThresholdMapper::toResponse)
                .toList();
    }

    @Transactional
    public ThresholdResponseDto createThreshold(UserPrincipal principal, UUID deviceId, CreateThresholdRequestDto request) {
        // Secure fetch - we need the Device entity to pass to the mapper
        Device device = authorizationService.getDeviceWithEditAccess(deviceId, principal);

        AlertThreshold threshold = alertThresholdMapper.createToEntity(request, device);
        threshold = alertThresholdRepository.save(threshold);

        thresholdCacheService.invalidateDeviceCache(deviceId);
        return alertThresholdMapper.toResponse(threshold);
    }

    @Transactional
    public ThresholdResponseDto updateThreshold(UserPrincipal principal, UUID deviceId, UUID thresholdId, UpdateThresholdRequestDto request) {
        AlertThreshold threshold = alertThresholdRepository.findByIdWithDevice(thresholdId)
                .orElseThrow(() -> new NotFoundException("Threshold with id " + thresholdId + " not found"));

        if (!threshold.getDevice().getId().equals(deviceId)) {
            throw new BadRequestException("Threshold " + thresholdId + " does not belong to device " + deviceId);
        }

        // We already have the device from the threshold, use old pattern
        authorizationService.verifyEditPermission(principal, threshold.getDevice());

        alertThresholdMapper.updateEntity(threshold, request);
        threshold = alertThresholdRepository.save(threshold);

        thresholdCacheService.invalidateDeviceCache(deviceId);
        return alertThresholdMapper.toResponse(threshold);
    }

    @Transactional
    public void deleteThreshold(UserPrincipal principal, UUID deviceId, UUID thresholdId) {
        AlertThreshold threshold = alertThresholdRepository.findByIdWithDevice(thresholdId)
                .orElseThrow(() -> new NotFoundException("Threshold with id " + thresholdId + " not found"));

        if (!threshold.getDevice().getId().equals(deviceId)) {
            throw new BadRequestException("Threshold " + thresholdId + " does not belong to device " + deviceId);
        }

        // We already have the device from the threshold, use old pattern
        authorizationService.verifyEditPermission(principal, threshold.getDevice());

        alertThresholdRepository.delete(threshold);
        thresholdCacheService.invalidateDeviceCache(deviceId);
    }
}