package com.phatdotdev.user_service.integration;

import com.phatdotdev.user_service.dto.UserDto;
import com.phatdotdev.user_service.repository.UserRepository;
import com.phatdotdev.user_service.testsupport.MySqlTestcontainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
public class UserServiceIntegrationTest
        extends MySqlTestcontainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_viaRestApi_persistsAndReturnsUser() {
        // 1. Prepare Request DTO
        UserDto userDto = UserDto.builder()
                .name("Phat")
                .surname("Do")
                .email("phat.dev@gmail.com")
                .address("Can Tho Viet Nam")
                .alerting(true)
                .energyAlertingThreshold(2000.0)
                .build();

        // 2. Execute POST Request
        ResponseEntity<UserDto> response = restTemplate
                .postForEntity("/api/v1/user", userDto, UserDto.class);

        // 3. Assert POST Response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UserDto responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isNotNull();
        assertThat(responseBody.getName()).isEqualTo(userDto.getName());
        assertThat(responseBody.getSurname()).isEqualTo(userDto.getSurname());
        assertThat(responseBody.getEmail()).isEqualTo(userDto.getEmail());
        assertThat(responseBody.getAddress()).isEqualTo(userDto.getAddress());
        assertThat(responseBody.isAlerting()).isEqualTo(userDto.isAlerting());
        assertThat(responseBody.getEnergyAlertingThreshold()).isEqualTo(userDto.getEnergyAlertingThreshold());

        // 4. Execute GET Request (Đã sửa lại thứ tự: URL -> ResponseType -> URI Variables)
        ResponseEntity<UserDto> loaded = restTemplate
                .getForEntity("/api/v1/user/{id}", UserDto.class, responseBody.getId());

        // 5. Assert GET Response
        assertThat(loaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loaded.getBody()).isNotNull();
        assertThat(loaded.getBody())
                .usingRecursiveComparison()
                .isEqualTo(responseBody);
    }
}