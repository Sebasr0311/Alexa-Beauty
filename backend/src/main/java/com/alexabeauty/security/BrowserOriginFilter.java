package com.alexabeauty.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Enforces exact browser origins and a non-simple header on cookie-authenticated writes. */
@Component
public class BrowserOriginFilter extends OncePerRequestFilter {
    private final List<String> origins;
    private final boolean requireOrigin;

    public BrowserOriginFilter(@Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> origins,
                               @Value("${app.auth.require-origin:false}") boolean requireOrigin) {
        this.origins = origins;
        this.requireOrigin = requireOrigin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean write = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        String origin = request.getHeader("Origin");
        boolean bearer = request.getHeader("Authorization") != null
                && request.getHeader("Authorization").startsWith("Bearer ");
        boolean cookie = request.getCookies() != null;
        if (write && request.getRequestURI().startsWith("/api/")
                && (origin != null || cookie || (requireOrigin && !bearer))) {
            if (origin == null || !origins.contains(origin)
                    || !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setStatus(403);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Untrusted browser request\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
