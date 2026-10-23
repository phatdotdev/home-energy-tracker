package com.phatdotdev.insight_service.client;

import com.phatdotdev.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class UsageClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UsageClient(RestTemplate restTemplate,
                       @Value("${usage.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public UsageDto getXDaysUsageForUser(Long userId, int days) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .queryParam("days", days)
                .buildAndExpand(userId)
                .toUriString();

        log.info("Calling Usage Service URL: {}", url);
        ResponseEntity<UsageDto> response = restTemplate.getForEntity(url, UsageDto.class);
        return response.getBody();
    }
}
