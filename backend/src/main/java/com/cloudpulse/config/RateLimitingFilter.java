package com.cloudpulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int authRateLimit;
    private final int apiRateLimit;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${app.rate-limit.auth-per-minute:15}") int authRateLimit,
            @Value("${app.rate-limit.api-per-minute:120}") int apiRateLimit,
            ObjectMapper objectMapper) {
        this.authRateLimit = authRateLimit;
        this.apiRateLimit = apiRateLimit;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = extractClientIp(request);

        Bucket bucket;
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            bucket = authBuckets.computeIfAbsent(clientIp, k -> createNewBucket(authRateLimit));
        } else if (path.startsWith("/api/")) {
            bucket = apiBuckets.computeIfAbsent(clientIp, k -> createNewBucket(apiRateLimit));
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> errorDetails = Map.of(
                    "success", false,
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "RATE_LIMIT_EXCEEDED",
                    "message", "Rate limit exceeded. Please try again in a moment.",
                    "timestamp", Instant.now().toString()
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        }
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
