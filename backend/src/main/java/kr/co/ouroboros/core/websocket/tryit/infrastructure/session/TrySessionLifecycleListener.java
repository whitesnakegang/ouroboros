package kr.co.ouroboros.core.websocket.tryit.infrastructure.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Cleans up Try mappings when STOMP session is disconnected.
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
        log.trace("Session {} disconnected. Cleaning up Try mappings.", sessionId);
        trySessionRegistry.removeBySessionId(sessionId);
    }
}

