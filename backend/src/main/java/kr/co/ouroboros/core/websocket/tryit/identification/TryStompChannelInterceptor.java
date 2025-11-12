package kr.co.ouroboros.core.websocket.tryit.identification;

import io.opentelemetry.context.Scope;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders;
import kr.co.ouroboros.core.websocket.tryit.infrastructure.messaging.TryPublisherNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Interceptor that identifies Try requests in STOMP inbound messages and sets Try context.
 * <p>
 * Uses the same header protocol as {@link kr.co.ouroboros.core.rest.tryit.identification.TryFilter}
 * to issue and propagate tryId in STOMP messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TryStompChannelInterceptor implements ChannelInterceptor {

    private final TryPublisherNotifier tryPublisherNotifier;

    /**
     * Intercepts inbound STOMP messages to establish and propagate a Try context when a Try is requested
     * or an explicit Try identifier is present.
     *
     * If a Try identifier is resolved, this method sets the Try context, stores the resulting scope (if any)
     * in the message headers, records the Try identifier in both native and standard headers, and — for SEND
     * frames with an explicit Try request — notifies the configured publisher.
     *
     * @param message the incoming STOMP message to inspect and potentially return with updated headers
     * @param channel the target message channel (not used by this implementation)
     * @return the original message if no Try processing occurred; otherwise a new Message with headers
     *         containing the Try identifier and, when set, an internal scope reference
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();
        if (command == null) {
            log.trace("Skipping Try check: STOMP command is missing");
            return message;
        }

        // Only inbound channel (client -> server) messages are processed. Additional filtering by command is possible.
        String tryHeader = getFirstNativeHeader(accessor, TryStompHeaders.TRY_HEADER);
        boolean tryRequested = TryStompHeaders.TRY_HEADER_ENABLED_VALUE.equalsIgnoreCase(tryHeader);

        UUID tryId = resolveTryId(accessor, tryRequested);
        if (tryId == null) {
            // If it's not a Try request or we couldn't issue a Try identifier, don't touch the context.
            return message;
        }

        Scope scope = TryContext.setTryId(tryId);
        if (scope != null) {
            accessor.setHeader(TryStompHeaders.INTERNAL_SCOPE_HEADER, scope);
        }

        if (tryRequested && StompCommand.SEND.equals(command)) {
            tryPublisherNotifier.notifyPublisher(tryId, accessor, message);
        }

        // Record tryId in frame header so the identifier is maintained in subsequent transmissions.
        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());

        log.trace("Set Try context in STOMP {} frame. tryId={}", command, tryId);

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    /**
     * Closes any Try context Scope attached to the STOMP message after send completion.
     *
     * If the message has a StompHeaderAccessor and an INTERNAL_SCOPE_HEADER containing a `Scope`,
     * this method attempts to close that `Scope`. Any exception thrown while closing is caught and
     * logged as a warning. If no accessor or scope is present, the method returns without action.
     *
     * @param message the STOMP message whose completion is being handled
     * @param channel the message channel used to send the message
     * @param sent true if the message was successfully sent, false otherwise
     * @param ex an exception that occurred during send, or `null` if none
     */
    @Override
    public void afterSendCompletion(
            Message<?> message,
            MessageChannel channel,
            boolean sent,
            @Nullable Exception ex
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return;
        }

        Scope scope = (Scope) accessor.getHeader(TryStompHeaders.INTERNAL_SCOPE_HEADER);
        if (scope != null) {
            try {
                scope.close();
            } catch (Exception e) {
                log.warn("Exception while cleaning up Try STOMP Scope: {}", e.getMessage());
            }
        }
    }

    /**
     * Resolve the Try identifier for the provided STOMP frame, preferring an explicit header or generating one when a Try is requested.
     *
     * @param accessor    the STOMP header accessor for the current message
     * @param tryRequested true if the frame requested a Try via the TRY header
     * @return             the resolved `UUID` from the `TRY_ID_HEADER` if present and valid, a newly generated `UUID` when `tryRequested` is true, or `null` if no Try identifier is available
     */
    @Nullable
    private UUID resolveTryId(StompHeaderAccessor accessor, boolean tryRequested) {
        // 1) If tryId is included in frame header, use it as is
        String headerTryId = getFirstNativeHeader(accessor, TryStompHeaders.TRY_ID_HEADER);
        if (headerTryId != null) {
            try {
                return UUID.fromString(headerTryId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tryId format in STOMP header: {}", headerTryId);
            }
        }

        // 2) If Try request header is enabled, generate a new one
        if (tryRequested) {
            UUID newTryId = UUID.randomUUID();
            log.debug("Detected STOMP Try request and generated new tryId: {}", newTryId);
            return newTryId;
        }

        return null;
    }

    /**
     * Retrieve the first native STOMP header value for the given header name, or null if none exists.
     *
     * @param accessor   the STOMP header accessor to read native headers from
     * @param headerName the native header name to look up
     * @return the first native header value for {@code headerName}, or {@code null} if the header is absent or has no values
     */
    @Nullable
    private String getFirstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        if (accessor.getNativeHeader(headerName) == null || accessor.getNativeHeader(headerName).isEmpty()) {
            return null;
        }
        return accessor.getFirstNativeHeader(headerName);
    }
}

