package com.alexabeauty.auth;

import com.alexabeauty.auth.domain.Role;
import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.dto.LoginRequest;
import com.alexabeauty.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Ensure the seeded admin password matches 'Admin123*Alexa'
        userRepository.findByEmail("admin@alexabeauty.com").ifPresent(admin -> {
            if (!passwordEncoder.matches("Admin123*Alexa", admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode("Admin123*Alexa"));
                userRepository.save(admin);
            }
        });
    }

    @Test
    @DisplayName("Login with valid admin credentials returns 200, JWT token, and profile")
    void login_withValidCredentials_shouldReturn200AndToken() throws Exception {
        LoginRequest request = new LoginRequest("admin@alexabeauty.com", "Admin123*Alexa");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("admin@alexabeauty.com"))
                .andExpect(jsonPath("$.data.user.role").value(Role.ADMIN.name()));
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void login_withInvalidPassword_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("admin@alexabeauty.com", "WrongPassword*123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("UNAUTHORIZED"));
    }

    @Test
    void httpOnlySessionAuthenticatesWithoutBrowserTokenStorage() throws Exception {
        var login = mockMvc.perform(post("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin@alexabeauty.com", "Admin123*Alexa"))))
                .andExpect(status().isOk()).andReturn();
        assertThat(login.getResponse().getHeader("Set-Cookie")).contains("HttpOnly", "SameSite=Lax");
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).path("data").path("token").asText();
        mockMvc.perform(get("/api/v1/auth/me").cookie(new jakarta.servlet.http.Cookie("token", token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"));
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new jakarta.servlet.http.Cookie("token", token))
                .header("Origin", "https://untrusted.example").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isForbidden());
        var logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(new jakarta.servlet.http.Cookie("token", token))
                .header("Origin", "http://localhost:3000").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk()).andReturn();
        assertThat(logout.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0", "HttpOnly");
    }

    @Test
    @DisplayName("Access to /api/v1/auth/me with valid Bearer token returns user profile")
    void getProfile_withValidToken_shouldReturnProfile() throws Exception {
        // Step 1: Login to get token
        LoginRequest loginRequest = new LoginRequest("admin@alexabeauty.com", "Admin123*Alexa");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = jsonNode.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // Step 2: Use token to query /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@alexabeauty.com"))
                .andExpect(jsonPath("$.data.role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("Access to protected /api/v1/admin/orders without token returns 401 Unauthorized")
    void accessProtectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access to /api/v1/health/public is allowed without token")
    void accessHealthPublic_withoutToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health/public")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Logout endpoint invalidates session cookie and returns 200")
    void logout_shouldReturn200() throws Exception {
        // Step 1: Login
        LoginRequest loginRequest = new LoginRequest("admin@alexabeauty.com", "Admin123*Alexa");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = jsonNode.path("data").path("token").asText();

        // Step 2: Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
