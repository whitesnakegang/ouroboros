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
 * Component for delivering Try request metadata to the publisher.
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
            log.debug("Cannot send Try publisher notification: session ID is missing. tryId={}", tryId);
            return;
        }

        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            log.warn("Failed to send Try notification: could not get SimpMessagingTemplate. sessionId={}, tryId={}", sessionId, tryId);
            return;
        }

        TryDispatchMessage dispatchMessage = buildDispatchMessage(accessor, message);
        trySessionRegistry.register(tryId, new TrySessionRegistry.TrySessionRegistration(sessionId, dispatchMessage));

        Map<String, Object> headers = createHeaders(sessionId);
        messagingTemplate.convertAndSendToUser(sessionId, PUBLISHER_DESTINATION, dispatchMessage, headers);
        log.trace("Sent Try notification to session {}. tryId={}", sessionId, tryId);
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
        // Explicitly add destination if it's not in headers
        // (destination is included in STOMP message headers, so client may need it)
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

