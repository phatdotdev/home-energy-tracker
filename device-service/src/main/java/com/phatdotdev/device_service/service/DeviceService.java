package com.phatdotdev.device_service.service;

import com.phatdotdev.device_service.dto.DeviceDto;
import com.phatdotdev.device_service.entity.Device;
import com.phatdotdev.device_service.exception.DeviceNotFoundException;
import com.phatdotdev.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {
    private DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository){
        this.deviceRepository = deviceRepository;
    }

    public DeviceDto getDeviceById(Long id){
        return deviceRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id "+id));
    }

    public DeviceDto createDevice(DeviceDto deviceDto){
        Device device = Device.builder()
                .name(deviceDto.getName())
                .type(deviceDto.getType())
                .location(deviceDto.getLocation())
                .userId(deviceDto.getUserId())
                .build();
        final Device savedDevice = deviceRepository.save(device);
        return toDto(savedDevice);
    }

    public DeviceDto updateDevice(Long id, DeviceDto deviceDto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id" + id));

        device.setName(deviceDto.getName());
        device.setType(deviceDto.getType());
        device.setLocation(deviceDto.getLocation());
        device.setUserId(deviceDto.getUserId());

        deviceRepository.save(device);

        return toDto(device);
    }

    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id" + id));
        deviceRepository.delete(device);
    }

    public List<DeviceDto> getAllDevicesByUserId(Long userId) {
        List<Device> devices = deviceRepository.findAllByUserId(userId);
        return devices.stream().map(this::toDto).toList();
    }

    private DeviceDto toDto(Device device){
        return DeviceDto.builder()
                .id(device.getId())
                .name(device.getName())
                .location(device.getLocation())
                .type(device.getType())
                .userId(device.getUserId())
                .energyConsumed(0.0)
                .build();
    }
}
