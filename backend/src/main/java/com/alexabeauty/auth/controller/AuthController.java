package com.alexabeauty.auth.controller;

import com.alexabeauty.auth.dto.AuthResponse;
import com.alexabeauty.auth.dto.LoginRequest;
import com.alexabeauty.auth.dto.UserProfileResponse;
import com.alexabeauty.auth.service.AuthService;
import com.alexabeauty.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticación y control de sesión")
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.auth.cookie-same-site:Lax}")
    private String cookieSameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve el token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);

        // Set HttpOnly cookie for web security
        ResponseCookie cookie = ResponseCookie.from("token", authResponse.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(authResponse.getExpiresIn())
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.ok("Sesión iniciada exitosamente", authResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida la cookie de sesión del usuario")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.empty("Sesión finalizada exitosamente"));
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Retorna los datos del usuario actualmente autenticado")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse profile = authService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok("Perfil consultado exitosamente", profile));
    }
}
