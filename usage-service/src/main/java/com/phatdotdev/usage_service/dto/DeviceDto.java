package com.phatdotdev.usage_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDto {
    private Long id;
    private String name;
    private String type;
    private String location;
    private Long userId;
    @Builder.Default
    private Double energyConsumed = 0.0;
}
