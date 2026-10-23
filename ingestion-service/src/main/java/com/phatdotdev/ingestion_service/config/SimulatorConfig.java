package com.phatdotdev.ingestion_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class SimulatorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService simulatorExecutor(
            @Value("${simulation.parallel-threads}") int parallelThreads) {
        return Executors.newFixedThreadPool(parallelThreads);
    }
}