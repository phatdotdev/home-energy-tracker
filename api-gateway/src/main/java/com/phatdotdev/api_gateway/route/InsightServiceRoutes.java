package com.phatdotdev.api_gateway.route;

import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

public class InsightServiceRoutes {
    @Bean
    RouterFunction<ServerResponse> insightRoutes() {
        return route("insight-service")
                .route(path("/api/v1/insight/**"), http())
                .before(uri("http://localhost:8085"))
                .filter(CircuitBreakerFilterFunctions.circuitBreaker(
                        "insightServiceCircuitBreaker",
                        URI.create("fallback:/fallbackRoute")
                ))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> insightFallbackRoute(){
        return route("fallbackRoute")
                .route(path("/fallbackRoute"),
                        request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Device service is down"))
                .build();
    }

}
