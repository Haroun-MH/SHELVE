package com.shelve.gateway.config;

import com.shelve.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authFilter;

    public GatewayConfig(AuthenticationFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth routes - no authentication required
                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))
                
                // User profile routes - authentication required
                .route("auth-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("lb://auth-service"))
                
                // Book catalog - public read, auth for write
                .route("book-catalog-service", r -> r
                        .path("/api/books/**")
                        .uri("lb://book-catalog-service"))
                
                // Shelf service - authentication required
                .route("shelf-service", r -> r
                        .path("/api/shelves/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("lb://shelf-service"))
                
                // Review/Rating service - mixed auth
                .route("review-rating-service", r -> r
                        .path("/api/reviews/**", "/api/ratings/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("lb://review-rating-service"))
                
                // Recommendation service - authentication required
                .route("recommendation-service", r -> r
                        .path("/api/recommendations/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://recommendation-service:8085"))
                
                .build();
    }
}
