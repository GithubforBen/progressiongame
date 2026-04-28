package com.financegame.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding-window rate limiter for auth endpoints.
 * Allows at most MAX_REQUESTS attempts per IP within WINDOW_MS milliseconds.
 *
 * IP resolution:
 *   - By default the raw socket address (request.getRemoteAddr()) is used.
 *     Forwarded headers are NOT trusted, so a client cannot spoof its IP
 *     to bypass the limiter.
 *   - When the application is deployed behind a trusted reverse proxy
 *     (e.g. Cloudflare Tunnel, nginx) operators MUST set
 *     security.trust-forwarded-for=true so the real client IP is honoured.
 *     This setting should only be enabled when the proxy is guaranteed to
 *     overwrite (not append to) X-Forwarded-For from untrusted clients.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000;
    // Hard cap on tracked IPs to bound memory in case of high cardinality
    // (e.g. an attacker rotating client IPs).
    private static final int MAX_TRACKED_IPS = 100_000;

    private record Window(AtomicInteger count, long windowStart) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final boolean trustForwardedFor;

    public RateLimitFilter(
        @Value("${security.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        long now = System.currentTimeMillis();

        // Best-effort eviction to keep the map bounded. We drop the whole
        // map if it grows too large; the resulting brief loss of state is
        // preferable to unbounded memory growth, and legitimate clients
        // simply re-establish their window on the next request.
        if (windows.size() > MAX_TRACKED_IPS) {
            windows.clear();
        }

        Window window = windows.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart() >= WINDOW_MS) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (window.count().get() > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Zu viele Anfragen. Bitte warte eine Minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Take the leftmost entry (the original client) and trim it.
                // Operators MUST ensure the upstream proxy overwrites this
                // header before enabling trust-forwarded-for.
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
