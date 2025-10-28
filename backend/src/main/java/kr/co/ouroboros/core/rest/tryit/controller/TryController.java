package kr.co.ouroboros.core.rest.tryit.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.dto.CreateSessionResponse;
import kr.co.ouroboros.core.rest.tryit.dto.TryResultResponse;
import kr.co.ouroboros.core.rest.tryit.session.TrySession;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * REST API controller for Try session management.
 * Creates and retrieves Try sessions for QA analysis.
 */
@Slf4j
@RestController
@RequestMapping("/ouroboros/tries")
@RequiredArgsConstructor
public class TryController {
    
    private final TrySessionRegistry registry;
    private final TrySessionProperties properties;
    
    /** HTTP header name: X-Ouroboros-Try */
    private static final String HEADER_NAME = "X-Ouroboros-Try";
    
    /** OpenTelemetry Baggage key: ouro.try_id */
    private static final String BAGGAGE_KEY = "ouro.try_id";

    /**
     * Creates a new Try session.
     * 
     * POST /ouroboros/tries
     * 
     * Response:
     * - tryId: session identifier
     * - headerName: HTTP header name
     * - baggageKey: Baggage key
     * - expiresAt: expiration time
     * 
     * @param request HTTP request (for IP extraction)
     * @return created session information
     * @throws ResponseStatusException 429 if active sessions exceed maxActive
     */
    @PostMapping
    public CreateSessionResponse createSession(HttpServletRequest request) {
        // Check active session count
        if (registry.getActiveSessionCount() >= properties.getMaxActive()) {
            log.warn("Max active sessions exceeded: current={}, max={}", 
                    registry.getActiveSessionCount(), properties.getMaxActive());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                    "Maximum active sessions exceeded");
        }
        
        UUID tryId = UUID.randomUUID();
        String clientIp = getClientIp(request);
        
        TrySession session = registry.register(
                tryId, 
                clientIp, 
                properties.getTtlSeconds(), 
                properties.isOneShot()
        );
        
        log.info("Created try session: tryId={}, clientIp={}, expiresAt={}", 
                tryId, clientIp, session.getExpiresAt());
        
        return new CreateSessionResponse(
                tryId.toString(),
                HEADER_NAME,
                BAGGAGE_KEY,
                session.getExpiresAt()
        );
    }
    
    /**
     * Retrieves Try result.
     * 
     * GET /ouroboros/tries/{tryId}
     * 
     * @param tryIdStr Try session ID
     * @return analysis result
     */
    @GetMapping("/{tryId}")
    public TryResultResponse getResult(@PathVariable("tryId") String tryIdStr) {
        UUID tryId;
        try {
            tryId = UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }
        
        TrySession session = registry.getSession(tryId);
        if (session == null) {
            log.debug("Try session not found: tryId={}", tryId);
            return TryResultResponse.builder()
                    .tryId(tryIdStr)
                    .status(TryResultResponse.Status.NOT_FOUND)
                    .error("Try session not found or expired")
                    .build();
        }
        
        // TODO: Integrate with Tempo and perform analysis
        // For now, return a placeholder response
        log.info("Retrieving result for tryId: {}", tryId);
        return TryResultResponse.builder()
                .tryId(tryIdStr)
                .status(TryResultResponse.Status.PENDING)
                .createdAt(session.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .analyzedAt(Instant.now())
                .issues(new ArrayList<>())
                .spanCount(0)
                .build();
    }
    
    /**
     * Extracts client IP address from HTTP request.
     * Checks X-Forwarded-For header first, then uses RemoteAddr.
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
