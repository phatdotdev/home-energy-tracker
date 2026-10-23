package com.phatdotdev.ingestion_service.simulator;

import com.phatdotdev.ingestion_service.dto.EnergyUsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

@Slf4j
@Component
public class ContinuousDataSimulator implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    @Value(
            "${simulation.requests-per-interval}"
    )
    private int requestsPerInterval;

    @Value(
            "${simulation.endpoint}"
    )
    String ingestionEndpoint;

    @Override
    public void run(String... args) throws Exception {

    }

//    @Scheduled(fixedRateString = "${simulation.interval-ms}")
    public void sendMockData(){
        for(int i = 1; i < requestsPerInterval; i++){
            EnergyUsageDto dto = EnergyUsageDto.builder()
                    .deviceId(random.nextLong(5, 12))
                    .energyConsumed(Math.round(random.nextDouble(0.0, 20.0) * 100.0)/100.0)
                    .timestamp(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
                    .build();

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<EnergyUsageDto> request = new HttpEntity<>(dto, headers);
                restTemplate.postForEntity(ingestionEndpoint, request, Void.class);
            }catch (RuntimeException error){
                log.info("Failed to send data: {}", error.getMessage());
            }
        }
    }
}
