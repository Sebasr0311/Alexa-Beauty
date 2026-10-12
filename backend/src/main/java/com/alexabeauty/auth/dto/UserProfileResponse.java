package com.alexabeauty.auth.dto;

import com.alexabeauty.auth.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private boolean active;
    private Instant lastLoginAt;
    private Instant createdAt;
}
