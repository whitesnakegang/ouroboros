package kr.co.ouroboros.core.websocket.tryit.identification;

import io.opentelemetry.context.Scope;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders;
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
 * Interceptor that adds tryId header to STOMP messages sent from server to client.
 */
@Slf4j
@Component
public class TryStompOutboundChannelInterceptor implements ChannelInterceptor {

    /**
     * Ensures an outbound STOMP message carries a tryId and establishes a per-message try scope when present.
     *
     * If a tryId is already present in TryContext it is propagated; otherwise the method attempts to extract a tryId
     * from the message headers and, if found, sets it in TryContext and stores the resulting Scope in an internal header.
     * When the STOMP command is missing, a new MESSAGE accessor is created and all original headers (including native and
     * important Spring messaging headers) are preserved so the message can be encoded correctly. The tryId is added as
     * both a native header and a regular header before returning a message with the updated headers.
     *
     * @param message the outbound message to inspect and possibly modify
     * @param channel the message channel the message will be sent to
     * @return the original message if no tryId is found; otherwise a message with the tryId added (and with a rebuilt
     *         accessor if the original STOMP command was missing)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        accessor.setLeaveMutable(true);
        if (log.isTraceEnabled()) {
            log.trace("Outbound preSend invoked. simpSessionId={}, simpUser={}, headers={}", accessor.getSessionId(), accessor.getUser(), accessor.toMap());
        }

        UUID tryId = TryContext.getTryId();
        if (tryId == null) {
            tryId = extractTryIdFromHeaders(accessor);
            if (tryId == null) {
                log.trace("Could not find tryId in TryContext and headers. headers={}", accessor.toMap());
                return message;
            }
            Scope scope = TryContext.setTryId(tryId);
            if (scope != null) {
                accessor.setHeader(TryStompHeaders.INTERNAL_OUTBOUND_SCOPE_HEADER, scope);
            }
        }

        // Set to MESSAGE if StompCommand is missing (required by StompEncoder)
        // Brokers may not set command when creating broadcast messages
        StompCommand command = accessor.getCommand();
        boolean needNewAccessor = (command == null);
        
        if (needNewAccessor) {
            // If command is missing, create new accessor and preserve all headers from original message
            // Especially copy Spring messaging headers (simpSubscriptionId, simpDestination, etc.) explicitly
            StompHeaderAccessor newAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
            newAccessor.setLeaveMutable(true);
            
            // Copy all headers from original message (including Spring messaging headers)
            // Copy basic headers with copyHeaders(), then set Spring messaging headers explicitly
            newAccessor.copyHeaders(message.getHeaders());
            
            // Copy important Spring messaging headers from original accessor explicitly
            // This allows StompEncoder to properly encode subscription, destination, etc.
            String subscriptionId = accessor.getSubscriptionId();
            String destination = accessor.getDestination();
            String sessionId = accessor.getSessionId();
            String messageId = accessor.getMessageId();
            
            if (subscriptionId != null) {
                newAccessor.setSubscriptionId(subscriptionId);
            }
            if (destination != null) {
                newAccessor.setDestination(destination);
            }
            if (sessionId != null) {
                newAccessor.setSessionId(sessionId);
            }
            if (messageId != null) {
                newAccessor.setMessageId(messageId);
            }
            
            // Copy native headers from original message (content-type, etc.)
            accessor.toNativeHeaderMap().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    newAccessor.setNativeHeader(key, values.get(0));
                }
            });
            
            // Add tryId header
            newAccessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
            newAccessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
            
            log.trace("Set tryId({}) header on outbound STOMP message (no command, new accessor created). subscriptionId={}, destination={}", 
                    tryId, newAccessor.getSubscriptionId(), newAccessor.getDestination());
            
            // Recreate message with new accessor headers
            // Keep original message payload and only update headers
            return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
        }
        
        // If command exists, only add tryId header to original accessor
        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        
        log.trace("Set tryId({}) header on outbound STOMP message. command={}, destination={}, subscriptionId={}", 
                tryId, accessor.getCommand(), accessor.getDestination(), accessor.getSubscriptionId());

        // Preserve all headers from original message and only add tryId header
        // Accessor is mutable, so changes are already reflected
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    /**
     * Cleans up any outbound try scope stored on the message and logs transmission failures.
     *
     * <p>If the message contains a Scope under {@code TryStompHeaders.INTERNAL_OUTBOUND_SCOPE_HEADER},
     * that scope is closed; exceptions thrown while closing are caught and logged. If the message
     * transmission was not completed, a warning with session and header information is logged.</p>
     *
     * @param message the outbound message that may carry an internal outbound scope and STOMP headers
     * @param channel the channel the message was sent to
     * @param sent    `true` if the message transmission completed successfully, `false` otherwise
     * @param ex      the exception thrown during send, or {@code null} if none
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
        Object scopeObject = accessor.getHeader(TryStompHeaders.INTERNAL_OUTBOUND_SCOPE_HEADER);
        if (scopeObject instanceof Scope scope) {
            try {
                scope.close();
            } catch (Exception e) {
                log.warn("Exception while cleaning up Outbound Try Scope: {}", e.getMessage());
            }
        }

        if (!sent) {
            log.warn("STOMP message transmission not completed. simpSessionId={}, headers={}, exception={}", accessor.getSessionId(), accessor.toMap(), ex != null ? ex.getMessage() : "none");
        }
    }

    /**
     * Extracts a tryId UUID from the accessor's native STOMP headers.
     *
     * @param accessor the STOMP header accessor to read native headers from
     * @return the UUID parsed from the TRY_ID header, or null if the header is missing or not a valid UUID
     */
    private UUID extractTryIdFromHeaders(StompHeaderAccessor accessor) {
        String headerTryId = accessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER);
        if (headerTryId != null) {
            try {
                return UUID.fromString(headerTryId);
            } catch (IllegalArgumentException e) {
                log.warn("tryId value in outbound STOMP header is not in UUID format: {}", headerTryId);
            }
        }

        return null;
    }
}

