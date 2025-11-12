package kr.co.ouroboros.core.websocket.tryit.identification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * STOMP 세션 종료 시 Try 매핑을 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrySessionLifecycleListener {

    private final TrySessionRegistry trySessionRegistry;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        log.trace("세션 {} 종료 감지. Try 매핑을 정리합니다.", sessionId);
        trySessionRegistry.removeBySessionId(sessionId);
    }
}

