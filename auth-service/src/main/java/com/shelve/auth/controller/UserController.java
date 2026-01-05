package com.shelve.auth.controller;

import com.shelve.auth.dto.ChangePasswordRequest;
import com.shelve.auth.dto.UpdateProfileRequest;
import com.shelve.auth.dto.UserProfileResponse;
import com.shelve.auth.dto.UserInfoResponse;
import com.shelve.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(authService.getProfile(userId));
    }
    
    @GetMapping("/{userId}/info")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserInfo(userId));
    }
    
    @PostMapping("/batch/info")
    public ResponseEntity<Map<String, UserInfoResponse>> getUserInfoBatch(@RequestBody List<String> userIds) {
        return ResponseEntity.ok(authService.getUserInfoBatch(userIds));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/onboarding/complete")
    public ResponseEntity<Void> completeOnboarding(@RequestHeader("X-User-Id") String userId) {
        authService.completeOnboarding(userId);
        return ResponseEntity.ok().build();
    }
}
