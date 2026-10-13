package com.alexabeauty.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class PublicWriteRateLimitFilterTest {
    @Test
    void boundsLoginAndCheckoutWithoutTrustingSpoofedForwardedAddresses() throws Exception {
        var filter = new PublicWriteRateLimitFilter(true, 2, 3);
        for (int i = 0; i < 3; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "203.0.113." + i);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertThat(response.getStatus()).isEqualTo(i < 2 ? 200 : 429);
            if (i == 2) assertThat(response.getHeader("Retry-After")).isNotNull();
        }
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/catalog/products"), response, (req, res) -> {});
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
