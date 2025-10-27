package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that identifies Try requests.
 * Checks X-Ouroboros-Try header and validates against TrySessionRegistry.
 * 
 * Behavior:
 * 1. If X-Ouroboros-Try header exists, treat as tryId
 * 2. Validate with TrySessionRegistry
 * 3. If valid, set tryId in Baggage (for OpenTelemetry integration)
 * 4. If invalid or missing, process as normal request
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TryFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Ouroboros-Try";
    private static final String BAGGAGE_KEY = "ouro.try_id";
    
    private final TrySessionRegistry registry;
    private final TrySessionProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String tryIdHeader = request.getHeader(HEADER_NAME);
        
        if (tryIdHeader != null && !tryIdHeader.isEmpty()) {
            // Try request identified
            try {
                UUID tryId = UUID.fromString(tryIdHeader);
                String clientIp = getClientIp(request);
                
                // Validate session
                if (registry.isValid(tryId, clientIp, properties.isBindClientIp())) {
                    log.debug("Valid Try request detected: tryId={}, clientIp={}", tryId, clientIp);
                    
                    // Set tryId in ThreadLocal context for OpenTelemetry integration
                    TryContext.setTryId(tryId);
                    
                    // Mark as used if oneShot mode
                    if (properties.isOneShot()) {
                        registry.markUsed(tryId);
                    }
                } else {
                    log.debug("Invalid Try request: tryId={}, clientIp={}", tryId, clientIp);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in X-Ouroboros-Try header: {}", tryIdHeader);
            }
        }
        
        try {
            // Continue request processing
            filterChain.doFilter(request, response);
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            TryContext.clear();
        }
    }
    
    /**
     * Extracts client IP address from HTTP request.
     * 
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    

}
