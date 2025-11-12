package kr.co.ouroboros.core.websocket.tryit.infrastructure.messaging;

import kr.co.ouroboros.core.websocket.tryit.infrastructure.session.TrySessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Try 요청 발행자에게 메타데이터를 전달하는 컴포넌트.
 */
@Slf4j
@Component
public class TryPublisherNotifier {

    private static final String PUBLISHER_DESTINATION = "/queue/try";

    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final TrySessionRegistry trySessionRegistry;

    public TryPublisherNotifier(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider, TrySessionRegistry trySessionRegistry) {
        this.messagingTemplateProvider = messagingTemplateProvider;
        this.trySessionRegistry = trySessionRegistry;
    }

    public void notifyPublisher(UUID tryId, StompHeaderAccessor accessor, Message<?> message) {
        if (tryId == null) {
            return;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            log.debug("세션 ID가 없어 Try 발행자 알림을 보낼 수 없습니다. tryId={}", tryId);
            return;
        }

        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            log.warn("SimpMessagingTemplate을 가져오지 못해 Try 알림을 전송하지 못했습니다. sessionId={}, tryId={}", sessionId, tryId);
            return;
        }

        TryDispatchMessage dispatchMessage = buildDispatchMessage(accessor, message);
        trySessionRegistry.register(tryId, new TrySessionRegistry.TrySessionRegistration(sessionId, dispatchMessage));

        Map<String, Object> headers = createHeaders(sessionId);
        messagingTemplate.convertAndSendToUser(sessionId, PUBLISHER_DESTINATION, dispatchMessage, headers);
        log.trace("세션 {}에 Try 알림을 전송했습니다. tryId={}", sessionId, tryId);
    }

    private TryDispatchMessage buildDispatchMessage(StompHeaderAccessor accessor, Message<?> message) {
        String payload = extractPayload(message.getPayload());
        Map<String, String> headers = extractHeaders(accessor);
        return new TryDispatchMessage(payload, headers);
    }

    private String extractPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return payload.toString();
    }

    private Map<String, String> extractHeaders(StompHeaderAccessor accessor) {
        Map<String, List<String>> nativeHeaders = accessor.toNativeHeaderMap();
        Map<String, String> flattened = new LinkedHashMap<>();
        if (nativeHeaders != null) {
            nativeHeaders.forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    flattened.put(key, values.get(0));
                }
            });
        }
        // destination이 headers에 없으면 명시적으로 추가
        // (destination은 STOMP 메시지 헤더에 포함되므로 클라이언트에서 필요할 수 있음)
        String destination = accessor.getDestination();
        if (destination != null && !flattened.containsKey("destination")) {
            flattened.put("destination", destination);
        }
        return flattened;
    }

    private Map<String, Object> createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}

