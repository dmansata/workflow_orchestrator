package com.airtribe.flow.interceptor;

import com.airtribe.flow.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed Rate Limiting Interceptor.
 *
 * How it works:
 *  1. Extracts the JWT from the Authorization header.
 *  2. Parses the username from the JWT (without full validation — SecurityConfig already does that).
 *  3. Uses a Redis key "rate_limit:<username>" as a sliding counter.
 *  4. On the first request in a window, sets a 60-second TTL on the key.
 *  5. On every subsequent request, increments the counter.
 *  6. If the counter exceeds MAX_REQUESTS_PER_MINUTE, the request is rejected with HTTP 429.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // Maximum requests allowed per user per 60-second window
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rate_limit:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String jwt = parseJwt(request);

        // If no JWT present, skip rate limiting (SecurityConfig will handle auth rejection)
        if (jwt == null) {
            return true;
        }

        String username;
        try {
            username = jwtUtils.getUserNameFromJwtToken(jwt);
        } catch (Exception e) {
            // Invalid token — let Spring Security handle the rejection downstream
            return true;
        }

        String redisKey = KEY_PREFIX + username;

        // INCR: Atomically increment the counter. Returns new value.
        Long requestCount = redisTemplate.opsForValue().increment(redisKey);

        if (requestCount == null) {
            // Redis error — fail open (allow the request)
            log.warn("Redis returned null for key {}. Failing open.", redisKey);
            return true;
        }

        // On the very first request in this window, set the TTL
        if (requestCount == 1) {
            redisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
            log.debug("Rate limit window started for user '{}'. Key: {}", username, redisKey);
        }

        // Add informational headers so clients can see their usage
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - requestCount)));
        response.setHeader("X-RateLimit-Reset", ttl != null ? String.valueOf(ttl) : "60");

        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit EXCEEDED for user '{}'. Count: {}/{}", username, requestCount, MAX_REQUESTS_PER_MINUTE);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"Rate limit exceeded\", \"message\": \"You have exceeded %d requests per minute. Please wait %s seconds.\"}",
                MAX_REQUESTS_PER_MINUTE, ttl != null ? ttl : "60"
            ));
            return false; // Block the request
        }

        log.debug("Rate limit OK for user '{}'. [{}/{}]", username, requestCount, MAX_REQUESTS_PER_MINUTE);
        return true; // Allow the request
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
