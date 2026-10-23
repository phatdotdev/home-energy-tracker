package com.phatdotdev.device_service.controller;

import com.phatdotdev.device_service.dto.DeviceDto;
import com.phatdotdev.device_service.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {

    private DeviceService deviceService;

    private DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id){
        DeviceDto deviceDto = deviceService.getDeviceById(id);
        return ResponseEntity.ok(deviceDto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDto>> getAllDeviceByUserId(
            @PathVariable Long userId
    ){
        return ResponseEntity.ok(deviceService.getAllDevicesByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(@RequestBody DeviceDto deviceDto){
        return ResponseEntity.ok(deviceService.createDevice(deviceDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(
            @PathVariable Long id,
            @RequestBody DeviceDto deviceDto
    ){
        return ResponseEntity.ok(deviceService.updateDevice(id, deviceDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id){
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
