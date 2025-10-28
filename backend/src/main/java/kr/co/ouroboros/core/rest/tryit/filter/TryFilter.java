package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that identifies Try requests.
 * Checks X-Ouroboros-Try header for "on" value and generates tryId automatically.
 * 
 * Behavior:
 * 1. If X-Ouroboros-Try header equals "on", generate tryId
 * 2. Set tryId in Baggage (for OpenTelemetry integration)
 * 3. Return tryId in response header X-Ouroboros-Try-Id
 * 4. If missing or not "on", process as normal request
 */
@Slf4j
@Component
public class TryFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Ouroboros-Try";
    private static final String RESPONSE_HEADER_NAME = "X-Ouroboros-Try-Id";
    private static final String TRY_VALUE = "on";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String tryHeader = request.getHeader(HEADER_NAME);
        UUID tryId = null;
        
        // Check if this is a Try request
        if (TRY_VALUE.equalsIgnoreCase(tryHeader)) {
            tryId = UUID.randomUUID();
            log.debug("Try request detected, generating tryId: {}", tryId);
            
            // Set tryId in ThreadLocal context for OpenTelemetry integration
            TryContext.setTryId(tryId);
            
            // Set response header BEFORE processing request
            // This ensures the header is added before response is committed
            response.setHeader(RESPONSE_HEADER_NAME, tryId.toString());
            log.debug("Added tryId to response header: {}", tryId);
        }
        
        try {
            // Continue request processing
            filterChain.doFilter(request, response);
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            TryContext.clear();
        }
    }
}
