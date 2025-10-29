package kr.co.ouroboros.core.rest.tryit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that identifies Try requests and sets tryId in ThreadLocal context.
 * Response modification is handled by TryResponseBodyAdvice for better safety.
 * 
 * Behavior:
 * 1. If X-Ouroboros-Try header equals "on", generate tryId
 * 2. Set tryId in ThreadLocal context (for OpenTelemetry integration)
 * 3. Response modification is handled by TryResponseBodyAdvice
 * 4. If missing or not "on", process as normal request
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 최고 우선순위로 설정
public class TryFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Ouroboros-Try";
    private static final String TRY_VALUE = "on";
    private static final String TRY_ID_ATTRIBUTE = "ouro.try_id";
    
    private final Tracer tracer;
    
    public TryFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String tryHeader = request.getHeader(HEADER_NAME);
        
        // Check if this is a Try request
        if (TRY_VALUE.equalsIgnoreCase(tryHeader)) {
            UUID tryId = UUID.randomUUID();
            log.debug("Try request detected, generating tryId: {}", tryId);
            
            // Set tryId in ThreadLocal context FIRST (before span creation)
            TryContext.setTryId(tryId);
            
            try {
                // Continue request processing - Spring will create spans automatically
                filterChain.doFilter(request, response);
            } finally {
                // Clean up ThreadLocal to prevent memory leaks
                TryContext.clear();
            }
        } else {
            // Normal request processing
            filterChain.doFilter(request, response);
        }
    }
}