package com.alexabeauty.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Bounded instance-wide safety budgets. Configure trusted edge per-client limits separately. */
@Component
public class PublicWriteRateLimitFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final int loginLimit;
    private final int checkoutLimit;
    private long minute = -1;
    private int logins;
    private int checkouts;

    public PublicWriteRateLimitFilter(@Value("${app.rate-limit.enabled:false}") boolean enabled,
                                     @Value("${app.rate-limit.login-per-minute:30}") int loginLimit,
                                     @Value("${app.rate-limit.checkout-per-minute:120}") int checkoutLimit) {
        if (loginLimit < 1 || checkoutLimit < 1) throw new IllegalArgumentException("Rate budgets must be positive");
        this.enabled = enabled; this.loginLimit = loginLimit; this.checkoutLimit = checkoutLimit;
    }
    private synchronized boolean admit(boolean login) {
        long current = System.nanoTime() / 60_000_000_000L;
        if (minute != current) { minute = current; logins = 0; checkouts = 0; }
        if (login) { if (logins >= loginLimit) return false; logins++; }
        else { if (checkouts >= checkoutLimit) return false; checkouts++; }
        return true;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean login = path.equals("/api/v1/auth/login");
        boolean checkout = path.equals("/api/v1/orders");
        if (enabled && "POST".equals(request.getMethod()) && (login || checkout) && !admit(login)) {
            response.setStatus(429); response.setHeader("Retry-After", "60");
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Please retry in one minute\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
