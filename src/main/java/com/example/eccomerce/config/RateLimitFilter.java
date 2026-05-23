package com.example.eccomerce.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterConfig rateLimiterConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);

        // Skip rate limiting for health check
        if (path.equals("/ping")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get identifier (userId if logged in, IP if not)
        String identifier = getIdentifier(request, ip);

        // Choose bucket based on endpoint
        Bucket bucket = chooseBucket(path, method, identifier, ip);

        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to consume token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded: {} | {} | wait: {}s",
                    identifier, path, waitSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(waitSeconds));
            response.getWriter().write(
                    "{\"error\": \"TOO_MANY_REQUESTS\"," +
                            "\"message\": \"Rate limit exceeded. Try again in " +
                            waitSeconds + " seconds\"}"
            );
        }
    }

    private Bucket chooseBucket(String path, String method,
                                String identifier, String ip) {
        // Login — by IP
        if (path.contains("/api/auth/login")) {
            return rateLimiterConfig.resolveLoginBucket(ip);
        }

        // Register — by IP
        if (path.contains("/api/auth/register")) {
            return rateLimiterConfig.resolveRegisterBucket(ip);
        }

        // Order placement — by userId
        if (path.contains("/api/orders/place")) {
            return rateLimiterConfig.resolveOrderBucket(identifier);
        }

        // Cart operations — by userId
        if (path.contains("/api/cart")) {
            return rateLimiterConfig.resolveCartBucket(identifier);
        }

        // Everything else — general API limit
        return rateLimiterConfig.resolveApiBucket(identifier);
    }

    private String getIdentifier(HttpServletRequest request, String ip) {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getPrincipal().toString(); // userId or email
        }
        return ip; // fallback to IP for unauthenticated requests
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For can have multiple IPs — take first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}