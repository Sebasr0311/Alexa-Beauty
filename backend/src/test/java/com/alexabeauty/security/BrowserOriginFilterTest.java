package com.alexabeauty.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BrowserOriginFilterTest {
    @Test
    void rejectsCrossSiteCookieWriteAndMissingHeader() throws Exception {
        var filter = new BrowserOriginFilter(List.of("https://shop.example.com"), true);
        for (String origin : List.of("https://attacker.example", "https://shop.example.com")) {
            var request = new MockHttpServletRequest("POST", "/api/v1/admin/orders");
            request.setCookies(new Cookie("token", "session")); request.addHeader("Origin", origin);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> { throw new AssertionError("must not pass"); });
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }
    @Test
    void acceptsConfiguredOriginWithCustomHeader() throws Exception {
        var filter = new BrowserOriginFilter(List.of("https://shop.example.com"), true);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("Origin", "https://shop.example.com");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> res.setContentType("accepted"));
        assertThat(response.getContentType()).isEqualTo("accepted");
    }
}
