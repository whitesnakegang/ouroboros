package kr.co.ouroboros.core.rest.tryit.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.ouroboros.core.rest.tryit.config.TrySessionProperties;
import kr.co.ouroboros.core.rest.tryit.dto.CreateSessionResponse;
import kr.co.ouroboros.core.rest.tryit.session.TrySession;
import kr.co.ouroboros.core.rest.tryit.session.TrySessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Try 세션 발급을 위한 REST API 컨트롤러.
 * UI가 세션을 요청하면 UUID를 생성하여 반환한다.
 */
@Slf4j
@RestController
@RequestMapping("/ouroboros/tries")
@RequiredArgsConstructor
public class TryController {
    
    private final TrySessionRegistry registry;
    private final TrySessionProperties properties;
    
    /** HTTP 헤더 이름: X-Ouroboros-Try */
    private static final String HEADER_NAME = "X-Ouroboros-Try";
    
    /** OpenTelemetry Baggage 키: ouro.try_id */
    private static final String BAGGAGE_KEY = "ouro.try_id";

    /**
     * 새로운 Try 세션을 발급한다.
     * 
     * POST /ouroboros/tries
     * 
     * 응답:
     * - tryId: 세션 식별자
     * - headerName: HTTP 헤더 이름
     * - baggageKey: Baggage 키
     * - expiresAt: 만료 시각
     * 
     * @param request HTTP 요청 객체 (IP 추출용)
     * @return 생성된 세션 정보
     * @throws ResponseStatusException 활성 세션 수가 maxActive를 초과할 경우 429
     */
    @PostMapping
    public CreateSessionResponse createSession(HttpServletRequest request) {
        // 활성 세션 수 체크
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
     * HTTP 요청에서 클라이언트 IP 주소를 추출한다.
     * X-Forwarded-For 헤더를 우선 확인하고, 없으면 RemoteAddr를 사용한다.
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
