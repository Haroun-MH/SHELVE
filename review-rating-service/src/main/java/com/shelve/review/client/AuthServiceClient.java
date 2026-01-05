package com.shelve.review.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;
    
    private static final String AUTH_SERVICE_URL = "http://auth-service:8081";
    private static final String CIRCUIT_BREAKER_NAME = "authService";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserInfoFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public UserInfo getUserInfo(String userId) {
        log.debug("Calling auth-service to get user info for userId: {}", userId);
        return webClientBuilder.build()
                .get()
                .uri(AUTH_SERVICE_URL + "/api/users/{userId}/info", userId)
                .retrieve()
                .bodyToMono(UserInfo.class)
                .block();
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserInfoBatchFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public Map<String, UserInfo> getUserInfoBatch(List<String> userIds) {
        log.debug("Calling auth-service to get batch user info for {} users", userIds.size());
        return webClientBuilder.build()
                .post()
                .uri(AUTH_SERVICE_URL + "/api/users/batch/info")
                .bodyValue(userIds)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, UserInfo>>() {})
                .block();
    }

    // Fallback methods for circuit breaker
    private UserInfo getUserInfoFallback(String userId, Throwable throwable) {
        log.warn("Circuit breaker fallback: Failed to fetch user info for userId: {}. Reason: {}", 
                userId, throwable.getMessage());
        // Return a default/unknown user info when auth-service is unavailable
        return new UserInfo(userId, "Unknown User", null);
    }

    private Map<String, UserInfo> getUserInfoBatchFallback(List<String> userIds, Throwable throwable) {
        log.warn("Circuit breaker fallback: Failed to fetch batch user info for {} users. Reason: {}", 
                userIds.size(), throwable.getMessage());
        // Return empty map when auth-service is unavailable
        return Collections.emptyMap();
    }
}
