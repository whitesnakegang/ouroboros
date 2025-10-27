package kr.co.ouroboros.core.rest.tryit.session;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for storing and managing Try sessions in memory.
 * Uses ConcurrentHashMap for thread safety.
 */
@Slf4j
@Component
public class TrySessionRegistry {
    
    private final ConcurrentHashMap<UUID, TrySession> sessions = new ConcurrentHashMap<>();
    
    @Getter
    private volatile int activeSessionCount = 0;

    /**
     * 새로운 Try 세션을 등록한다.
     *
     * @param tryId 세션 식별자
     * @param clientIp 클라이언트 IP 주소
     * @param ttlSeconds 세션 TTL(초)
     * @param oneShot 일회성 사용 여부 (현재는 로그만 남김)
     * @return 생성된 TrySession 객체
     */
    public TrySession register(UUID tryId, String clientIp, int ttlSeconds, boolean oneShot) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
        TrySession session = new TrySession(tryId, clientIp, expiresAt);
        sessions.put(tryId, session);
        activeSessionCount = sessions.size();
        log.debug("Registered try session: tryId={}, clientIp={}, expiresAt={}, oneShot={}", 
                tryId, clientIp, expiresAt, oneShot);
        return session;
    }

    /**
     * 세션을 조회한다.
     *
     * @param tryId 세션 식별자
     * @return TrySession 객체, 없으면 null
     */
    public TrySession getSession(UUID tryId) {
        TrySession session = sessions.get(tryId);
        if (session != null && session.isExpired()) {
            sessions.remove(tryId);
            activeSessionCount = sessions.size();
            return null;
        }
        return session;
    }
    
    /**
     * 세션이 유효한지 검증한다.
     * 만료되지 않았고, 사용되지 않았으며, IP가 일치하는지 확인한다.
     *
     * @param tryId 세션 식별자
     * @param clientIp 검증할 클라이언트 IP
     * @param bindClientIp IP 바인딩 활성화 여부
     * @return 유효성 여부
     */
    public boolean isValid(UUID tryId, String clientIp, boolean bindClientIp) {
        TrySession session = sessions.get(tryId);
        
        if (session == null) {
            log.debug("Try session not found: tryId={}", tryId);
            return false;
        }
        
        if (session.isExpired()) {
            log.debug("Try session expired: tryId={}", tryId);
            sessions.remove(tryId);
            activeSessionCount = sessions.size();
            return false;
        }
        
        if (session.isUsed()) {
            log.debug("Try session already used: tryId={}", tryId);
            return false;
        }
        
        if (bindClientIp && !session.matchesIp(clientIp)) {
            log.debug("Try session IP mismatch: tryId={}, expected={}, actual={}", 
                    tryId, session.getClientIp(), clientIp);
            return false;
        }
        
        return true;
    }

    /**
     * 세션을 사용됨으로 표시한다.
     * oneShot 모드에서 중복 사용을 방지하기 위해 사용된다.
     *
     * @param tryId 세션 식별자
     */
    public void markUsed(UUID tryId) {
        TrySession session = sessions.get(tryId);
        if (session != null) {
            session.markUsed();
            log.debug("Marked try session as used: tryId={}", tryId);
        }
    }

    /**
     * 만료된 세션을 정리한다.
     * 1분마다 자동 실행되며, 만료된 세션을 제거하고 활성 세션 수를 갱신한다.
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void cleanup() {
        int beforeSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        activeSessionCount = sessions.size();
        
        int removed = beforeSize - activeSessionCount;
        if (removed > 0) {
            log.debug("Cleaned up {} expired try sessions. Active sessions: {}", removed, activeSessionCount);
        }
    }
}
