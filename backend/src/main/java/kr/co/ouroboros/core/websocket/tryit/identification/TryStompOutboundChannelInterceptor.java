package kr.co.ouroboros.core.websocket.tryit.identification;

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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        accessor.setLeaveMutable(true);
        if (log.isTraceEnabled()) {
            log.trace("Outbound preSend invoked. simpSessionId={}, simpUser={}, headers={}", accessor.getSessionId(), accessor.getUser(), accessor.toMap());
        }

        // TryId is managed in Baggage context, which is automatically propagated across threads and async boundaries.
        // We only need to read from Baggage and add it to the STOMP message header for client propagation.
        UUID tryId = TryContext.getTryId();
        if (tryId == null) {
            log.trace("No tryId in TryContext, skipping tryId header addition. headers={}", accessor.toMap());
            return message;
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

        if (!sent) {
            log.warn("STOMP message transmission not completed. simpSessionId={}, headers={}, exception={}", accessor.getSessionId(), accessor.toMap(), ex != null ? ex.getMessage() : "none");
        }
    }
}


