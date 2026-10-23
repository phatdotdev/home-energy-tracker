package com.phatdotdev.ingestion_service.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EnergyUsageEvent {
    Long deviceId;
    double energyConsumed;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp;
}
