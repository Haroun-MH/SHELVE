package com.shelve.review.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;
    
    private static final String AUTH_SERVICE_URL = "http://auth-service:8081";

    public UserInfo getUserInfo(String userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(AUTH_SERVICE_URL + "/api/users/{userId}/info", userId)
                    .retrieve()
                    .bodyToMono(UserInfo.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch user info for userId: {}", userId, e);
            return null;
        }
    }

    public Map<String, UserInfo> getUserInfoBatch(List<String> userIds) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(AUTH_SERVICE_URL + "/api/users/batch/info")
                    .bodyValue(userIds)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, UserInfo>>() {})
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch batch user info", e);
            return Collections.emptyMap();
        }
    }
}
