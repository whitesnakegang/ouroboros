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

    /**
     * Creates a TryPublisherNotifier that delivers Try request metadata to a publisher and registers dispatch sessions.
     *
     * @param messagingTemplateProvider provider for a SimpMessagingTemplate used to send messages to publishers
     * @param trySessionRegistry registry used to record Try session dispatch registrations
     */
    public TryPublisherNotifier(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider, TrySessionRegistry trySessionRegistry) {
        this.messagingTemplateProvider = messagingTemplateProvider;
        this.trySessionRegistry = trySessionRegistry;
    }

    /**
     * Notify the publisher user session about a Try request and register the dispatch in the session registry.
     *
     * If the provided `tryId` is null, the session ID is missing, or a messaging template is unavailable,
     * the method returns without sending or registering anything. When successful, it registers a
     * TryDispatchMessage for the `tryId` in the TrySessionRegistry and sends the dispatch to the user
     * destination "/queue/try" for the originating WebSocket session.
     *
     * @param tryId    the identifier of the Try request to notify for
     * @param accessor the STOMP header accessor used to extract the originating session ID and headers
     * @param message  the original Spring Message whose payload and headers are forwarded to the publisher
     */
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

    /**
     * Constructs a TryDispatchMessage that packages the message payload and its STOMP headers.
     *
     * @param accessor the STOMP header accessor used to extract headers
     * @param message  the original message from which the payload is extracted
     * @return a TryDispatchMessage containing the extracted payload (or null) and a map of header names to values
     */
    private TryDispatchMessage buildDispatchMessage(StompHeaderAccessor accessor, Message<?> message) {
        String payload = extractPayload(message.getPayload());
        Map<String, String> headers = extractHeaders(accessor);
        return new TryDispatchMessage(payload, headers);
    }

    /**
     * Normalize a message payload to a string representation.
     *
     * @param payload the payload to convert
     * @return `null` if {@code payload} is null, the payload decoded as a UTF-8 string if it is a {@code byte[]},
     *         otherwise the result of {@code payload.toString()}.
     */
    private String extractPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return payload.toString();
    }

    /**
     * Flattens STOMP native headers into a map of header name to first header value and ensures the STOMP destination is present.
     *
     * @param accessor the STOMP header accessor containing native headers and destination
     * @return a linked map of header names to their first string value; includes a "destination" entry when the accessor has a destination and it was not present in the native headers
     */
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

    /**
     * Create STOMP message headers for sending a message on behalf of a specific WebSocket session.
     *
     * @param sessionId the WebSocket/STOMP session id to include in the headers
     * @return a headers map suitable for use with SimpMessagingTemplate (contains the session id and is left mutable)
     */
    private Map<String, Object> createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
