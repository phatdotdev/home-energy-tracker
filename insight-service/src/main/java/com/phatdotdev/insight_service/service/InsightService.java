package com.phatdotdev.insight_service.service;

import com.phatdotdev.insight_service.client.UsageClient;
import com.phatdotdev.insight_service.dto.DeviceDto;
import com.phatdotdev.insight_service.dto.InsightDto;
import com.phatdotdev.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InsightService {

    private final UsageClient usageClient;
    private final OllamaChatModel ollamaChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InsightService(UsageClient usageClient, OllamaChatModel ollamaChatModel) {
        this.usageClient = usageClient;
        this.ollamaChatModel = ollamaChatModel;
    }

    public InsightDto getSavingsTips(Long userId) {
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, 3);
        log.info("Usage data: {}", objectMapper.writeValueAsString(usageData));

        // 防 NullPointerException: Kiểm tra null an toàn cho danh sách devices
        List<DeviceDto> devices = Optional.ofNullable(usageData)
                .map(UsageDto::devices)
                .orElse(Collections.emptyList());

        double totalUsage = devices.stream()
//                .filter(d -> d.energyConsumed() != null)
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

        String promptText = String.format("""
                This is my total energy consumption over the past 3 days: %.2f kWh.
                How can I reduce my energy consumption?
                How does it compare to an average household?
                Please provide concise and practical tips.
                """, totalUsage);

        try {
            ChatResponse response = ollamaChatModel.call(new Prompt(promptText));

            String tips = (response != null && response.getResult() != null)
                    ? response.getResult().getOutput().getText()
                    : "Unable to generate tips at this moment.";

            return InsightDto.builder()
                    .userId(userId)
                    .tips(tips)
                    .energyUsage(totalUsage)
                    .build();
        } catch (Exception e) {
            log.error("Error calling Ollama AI model for userId {}: {}", userId, e.getMessage());
            return InsightDto.builder()
                    .userId(userId)
                    .tips("AI service is currently unavailable. Please try again later.")
                    .energyUsage(totalUsage)
                    .build();
        }
    }

    public InsightDto getOverView(Long userId) {
        // Fetch data from Usage Service
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, 3);

        // 防 NullPointerException: Kiểm tra null an toàn cho danh sách devices
        List<DeviceDto> devices = Optional.ofNullable(usageData)
                .map(UsageDto::devices)
                .orElse(Collections.emptyList());

        double totalUsage = devices.stream()
//                .filter(d -> d.energyConsumed() != null)
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();

        log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

        String promptText = String.format("""
                Analyse the following energy usage data for the past 3 days and provide a concise overview with actionable insights.
                User ID: %d
                Total Usage: %.2f kWh
                Devices Detail: %s
                """, userId, totalUsage, devices);

        try {
            ChatResponse response = ollamaChatModel.call(new Prompt(promptText));

            String tips = (response != null && response.getResult() != null)
                    ? response.getResult().getOutput().getText()
                    : "Unable to generate overview at this moment.";

            return InsightDto.builder()
                    .userId(userId)
                    .tips(tips)
                    .energyUsage(totalUsage)
                    .build();
        } catch (Exception e) {
            log.error("Error calling Ollama AI model for userId {}: {}", userId, e.getMessage());
            return InsightDto.builder()
                    .userId(userId)
                    .tips("AI service is currently unavailable. Please try again later.")
                    .energyUsage(totalUsage)
                    .build();
        }
    }
}