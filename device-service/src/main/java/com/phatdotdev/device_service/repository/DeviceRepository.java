package com.phatdotdev.device_service.repository;

import com.phatdotdev.device_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    public List<Device> findAllByUserId(Long userId);
}
