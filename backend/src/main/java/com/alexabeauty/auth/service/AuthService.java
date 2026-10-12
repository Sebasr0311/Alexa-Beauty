package com.alexabeauty.auth.service;

import com.alexabeauty.auth.dto.AuthResponse;
import com.alexabeauty.auth.dto.LoginRequest;
import com.alexabeauty.auth.dto.UserProfileResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserProfileResponse getProfile(String email);
}
