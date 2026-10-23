package com.phatdotdev.usage_service.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnergyUsageEvent {
    Long deviceId;
    double energyConsumed;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp;
}
