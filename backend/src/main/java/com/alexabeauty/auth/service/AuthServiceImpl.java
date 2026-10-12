package com.alexabeauty.auth.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.dto.AuthResponse;
import com.alexabeauty.auth.dto.LoginRequest;
import com.alexabeauty.auth.dto.UserProfileResponse;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.common.error.ApiException;
import com.alexabeauty.common.error.ErrorCodes;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCodes.UNAUTHORIZED,
                        "Credenciales de acceso inválidas"
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.UNAUTHORIZED,
                    "Credenciales de acceso inválidas"
            );
        }

        if (!user.isActive()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.UNAUTHORIZED,
                    "La cuenta de usuario se encuentra desactivada"
            );
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user);
        UserProfileResponse profile = mapToProfile(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(profile)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return mapToProfile(user);
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
