package com.shelve.auth.service;

import com.shelve.auth.dto.*;
import com.shelve.auth.entity.User;
import com.shelve.auth.exception.UserAlreadyExistsException;
import com.shelve.auth.exception.UserNotFoundException;
import com.shelve.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .firstLogin(user.isFirstLogin())
                .onboardingComplete(user.isOnboardingComplete())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserNotFoundException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        
        boolean isFirstLogin = user.isFirstLogin();
        
        // Update first login flag
        if (user.isFirstLogin()) {
            user.setFirstLogin(false);
            userRepository.save(user);
        }

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .firstLogin(isFirstLogin)
                .onboardingComplete(user.isOnboardingComplete())
                .build();
    }

    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .onboardingComplete(user.isOnboardingComplete())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .onboardingComplete(user.isOnboardingComplete())
                .build();
    }

    @Transactional
    public void completeOnboarding(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.setOnboardingComplete(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public UserInfoResponse getUserInfo(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return UserInfoResponse.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
    
    public java.util.Map<String, UserInfoResponse> getUserInfoBatch(java.util.List<String> userIds) {
        java.util.List<UUID> uuids = userIds.stream()
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
        
        java.util.List<User> users = userRepository.findAllById(uuids);
        
        return users.stream()
                .collect(java.util.stream.Collectors.toMap(
                        u -> u.getId().toString(),
                        u -> UserInfoResponse.builder()
                                .id(u.getId().toString())
                                .name(u.getName())
                                .email(u.getEmail())
                                .build()
                ));
    }
}
