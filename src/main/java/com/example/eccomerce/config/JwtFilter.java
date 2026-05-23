package com.example.eccomerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ Check internal service key first
        String serviceKey = request.getHeader("X-Internal-Service-Key");
        if (serviceKey != null && serviceKey.equals(internalServiceKey)) {
            // Valid internal service call — set system authentication
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "INTERNAL_SERVICE", null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // Normal JWT flow for user requests
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            jwtUtil.isValid(token);

            // Only allow ACCESS tokens, not REFRESH tokens
            if (!"ACCESS".equals(jwtUtil.extractType(token))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token type\"}");
                return;
            }

            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            if ("TOKEN_EXPIRED".equals(e.getMessage())) {
                response.getWriter().write("{\"error\": \"TOKEN_EXPIRED\", \"message\": \"Access token expired, please refresh\"}");
            } else {
                response.getWriter().write("{\"error\": \"TOKEN_INVALID\", \"message\": \"Invalid token\"}");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }
}
