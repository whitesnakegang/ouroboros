package kr.co.ouroboros.core.rest.tryit.identification;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that identifies Try requests and sets tryId in OpenTelemetry Baggage.
 * Response modification (header and body) is handled by TryResponseAdvice for better safety.
 * 
 * Behavior:
 * 1. If X-Ouroboros-Try header equals "on", generate tryId
 * 2. Set tryId in OpenTelemetry Baggage (for context propagation)
 * 3. Set tryId in response header early (as safety net)
 * 4. Response modification is handled by TryResponseAdvice
 * 5. If missing or not "on", process as normal request
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 최고 우선순위로 설정
public class TryFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Ouroboros-Try";
    private static final String TRY_VALUE = "on";
    private static final String RESPONSE_TRY_ID_HEADER = "X-Ouroboros-Try-Id";
    private static final String REQUEST_TRY_ID_ATTR = "kr.co.ouroboros.tryId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String tryHeader = request.getHeader(HEADER_NAME);
        boolean isTryRequest = TRY_VALUE.equalsIgnoreCase(tryHeader);
        boolean generatedHere = false;

        if (isTryRequest && request.getDispatcherType() == jakarta.servlet.DispatcherType.REQUEST) {
            UUID tryId = UUID.randomUUID();
            log.debug("Try request detected, generating tryId: {}", tryId);
            TryContext.setTryId(tryId);
            // Set header early to cover cases where response may be committed within the chain (e.g., 404 basic error)
            response.setHeader(RESPONSE_TRY_ID_HEADER, tryId.toString());
            request.setAttribute(REQUEST_TRY_ID_ATTR, tryId.toString());
            generatedHere = true;
        }

        try {
            // Continue request processing - Spring will create spans automatically
            filterChain.doFilter(request, response);
        } finally {
            // At the end (also for ERROR dispatch), ensure header is set if possible
            String tryIdStr = null;
            UUID tryId = TryContext.getTryId();
            if (tryId != null) {
                tryIdStr = tryId.toString();
            } else {
                Object v = request.getAttribute(REQUEST_TRY_ID_ATTR);
                if (v != null) tryIdStr = String.valueOf(v);
            }
            if (tryIdStr != null && !tryIdStr.isEmpty() && !response.isCommitted()) {
                response.setHeader(RESPONSE_TRY_ID_HEADER, tryIdStr);
            }
            // Clean up only if we created it here on the initial REQUEST dispatch
            if (generatedHere) {
                TryContext.clear();
            }
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // We want to run on ERROR dispatch too, to enforce header at the end
        return false;
    }
}

