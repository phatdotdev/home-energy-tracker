package com.phatdotdev.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userServiceApiDocs(){
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("User Service API")
                        .description("User service API for Home Energy Tracker")
                        .version("1.0.0"));
    }

    private static License getLiscense(){
        License license = new License();
        return license;
    }

    private static Contact getContact(){
        Contact contact = new Contact();
        return contact;
    }
}
