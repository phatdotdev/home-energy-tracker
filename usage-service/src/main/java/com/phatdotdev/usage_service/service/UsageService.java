package com.phatdotdev.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.phatdotdev.usage_service.client.DeviceClient;
import com.phatdotdev.usage_service.client.UserClient;
import com.phatdotdev.usage_service.dto.DeviceDto;
import com.phatdotdev.usage_service.dto.UsageDto;
import com.phatdotdev.usage_service.dto.UserDto;
import com.phatdotdev.usage_service.kafka.event.AlertingEvent;
import com.phatdotdev.usage_service.kafka.event.EnergyUsageEvent;
import com.phatdotdev.usage_service.model.Device;
import com.phatdotdev.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsageService {

    private final InfluxDBClient influxDBClient;
    private final DeviceClient deviceClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, AlertingEvent> kafkaTemplate;

    private String influxBucket;
    private String influxOrg;

    public UsageService(
            InfluxDBClient influxDBClient,
            DeviceClient deviceClient,
            UserClient userClient,
            KafkaTemplate<String, AlertingEvent> kafkaTemplate,
            @Value("${influx.bucket}") String influxBucket,
            @Value("${influx.org}") String influxOrg) {
        this.influxDBClient = influxDBClient;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;

        this.influxBucket = influxBucket;
        this.influxOrg = influxOrg;
    }

    @KafkaListener(topics = "energy-usage", groupId = "usage-service")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent){
        log.info("Received energy usage event: {}", energyUsageEvent);
        Point point = Point.measurement("energy-usage")
                .addTag("deviceId", String.valueOf(energyUsageEvent.getDeviceId()))
                .addField("energyConsumed", energyUsageEvent.getEnergyConsumed())
                .time(energyUsageEvent.getTimestamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(influxBucket, influxOrg, point);
    }


    @Scheduled(cron = "*/10 * * * * *")
    public void aggregateDeviceEnergyUsage() {
        final Instant now = Instant.now();
        final Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        String fluxQuery = String.format("""
            from(bucket: "%s")
                |> range(start: %s, stop: %s)
                |> filter(fn: (r) => r["_measurement"] == "energy-usage")
                |> filter(fn: (r) => r["_field"] == "energyConsumed")
                |> group(columns: ["deviceId"])
                |> sum(column: "_value")
            """, influxBucket, oneHourAgo.toString(), now.toString());

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);
        List<DeviceEnergy> deviceEnergies = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object deviceIdObj = record.getValueByKey("deviceId");
                Object valueObj = record.getValueByKey("_value");

                if (deviceIdObj != null) {
                    double energyConsumed = valueObj instanceof Number ? ((Number) valueObj).doubleValue() : 0.0;
                    deviceEnergies.add(
                            DeviceEnergy.builder()
                                    .deviceId(Long.valueOf(deviceIdObj.toString()))
                                    .energyConsumed(energyConsumed)
                                    .build()
                    );
                }
            }
        }

        log.info("Aggregated device energies count: {}", deviceEnergies.size());

        if (deviceEnergies.isEmpty()) {
            return;
        }

        for (DeviceEnergy deviceEnergy : deviceEnergies) {
            try {
                DeviceDto deviceResponse = deviceClient.getDeviceById(deviceEnergy.getDeviceId());
                if (deviceResponse != null && deviceResponse.getUserId() != null) {
                    deviceEnergy.setUserId(deviceResponse.getUserId()); // Sửa: getUserId()
                }
            } catch (Exception e) {
                log.error("Failed to fetch device info for ID: {}", deviceEnergy.getDeviceId(), e);
            }
        }

        List<DeviceEnergy> validDeviceEnergies = deviceEnergies.stream()
                .filter(de -> de.getUserId() != null)
                .toList();

        Map<Long, List<DeviceEnergy>> userDeviceEnergyMap = validDeviceEnergies.stream()
                .collect(Collectors.groupingBy(DeviceEnergy::getUserId));

        userDeviceEnergyMap.forEach((userId, devices) -> {
            try {
                UserDto user = userClient.getUserById(userId);

                if (user == null || user.id() == null || !Boolean.TRUE.equals(user.alerting())) {
                    log.warn("User not found or alerting disabled for ID: {}", userId);
                    return;
                }

                double totalConsumption = devices.stream()
                        .mapToDouble(DeviceEnergy::getEnergyConsumed)
                        .sum();

                Double threshold = user.energyAlertingThreshold();

                if (threshold != null && totalConsumption > threshold) {
                    log.info("ALERT: User ID {} exceeded energy threshold! Total: {}, Threshold: {}",
                            userId, totalConsumption, threshold);

                    AlertingEvent alertingEvent = AlertingEvent.builder()
                            .userId(userId)
                            .message("Energy consumption threshold exceeded")
                            .energyConsumed(totalConsumption)
                            .threshold(threshold)
                            .email(user.email())
                            .build();

                    kafkaTemplate.send("energy-alerts", alertingEvent);
                } else {
                    log.debug("User ID {} is within threshold. Total: {}, Threshold: {}",
                            userId, totalConsumption, threshold);
                }
            } catch (Exception e) {
                log.error("Error processing alert for user ID: {}", userId, e);
            }
        });
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        log.info("Getting usage for userId {} over past {} days", userId, days);
        final List<DeviceDto> devicesDto = deviceClient.getAllDevicesForUser(userId);

        if(devicesDto == null || devicesDto.isEmpty()){
            return UsageDto.builder()
                    .userId(userId)
                    .devices(null)
                    .build();
        }

        final List<Device> devices = devicesDto.stream()
                .filter(Objects::nonNull)
                .map(dto -> Device.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .type(dto.getType())
                        .location(dto.getLocation())
                        .userId(dto.getUserId())
                        .build())
                .toList();

        // Build  a set of device ids to filter on flux query
        List<String> devicesIdString = devices.stream()
                .map(Device::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();

        final Instant now = Instant.now();
        final Instant start = now.minus(days, ChronoUnit.DAYS);

        final String deviceFilter = devicesIdString.stream()
                .map(idStr -> String.format("r[\"deviceId\"] == \"%s\"", idStr))
                .collect(Collectors.joining(" or "));
        final String fluxQuery = String.format("""
            from(bucket: "%s")
                |> range(start: %s, stop: %s)
                |> filter(fn: (r) => r["_measurement"] == "energy-usage")
                |> filter(fn: (r) => r["_field"] == "energyConsumed")
                |> filter(fn: (r) => %s)
                |> group(columns: ["deviceId"])
                |> sum(column: "_value")
        """, influxBucket, start.toString(), now.toString(), deviceFilter);

        final Map<Long, Double> aggredatedMap = new HashMap<>();
        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery, influxOrg);

            for(FluxTable table: tables){
                for(FluxRecord record: table.getRecords()){
                    Object deviceIdObj = record.getValueByKey("deviceId");
                    String deviceIdStr = deviceIdObj == null ? null : deviceIdObj.toString();

                    if(deviceIdStr == null) continue;

                    Double energyConsumed = record.getValueByKey("_value") instanceof Number
                            ? ((Number) record.getValueByKey("_value")).doubleValue()
                            : 0.0;
                    try {
                        Long deviceId = Long.valueOf(deviceIdStr);
                        aggredatedMap.put(deviceId, aggredatedMap.getOrDefault(deviceId, 0.0) + energyConsumed);
                    } catch (NumberFormatException nfe){
                        log.warn("Failed to parse deviceId from flux record: {}", deviceIdStr);
                    }
                }
            }


        } catch (RuntimeException e){
            log.error("Failed to query InfluxDB for user {} usage over {} days: {}.", userId, days,e.getMessage());
            devices.forEach(
                    d -> d.setEnergyConsumed(0.0)
            );
            return UsageDto.builder()
                    .userId(userId)
                    .devices(null)
                    .build();
        }

        // populate aggregated energy consumed per device
        for(Device device: devices){
            if(device == null || device.getId() == null) continue;
            device.setEnergyConsumed(aggredatedMap.getOrDefault(device.getId(), 0.0));
        }
        log.info("Aggregated energy consumption for userId {}: {}", userId, aggredatedMap);

        final List<DeviceDto> resultDevices = devices.stream()
                .map(d -> DeviceDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .type(d.getType())
                        .location(d.getLocation())
                        .userId(d.getUserId())
                        .energyConsumed(d.getEnergyConsumed())
                        .build())
                .toList();
        return UsageDto.builder()
                .userId(userId)
                .devices(resultDevices)
                .build();

    }
}
