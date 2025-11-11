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
 * 서버에서 클라이언트로 전달되는 STOMP 메시지에 tryId 헤더를 보강하는 인터셉터.
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

        UUID tryId = TryContext.getTryId();
        if (tryId == null) {
            tryId = extractTryIdFromHeaders(accessor);
            if (tryId == null) {
                log.trace("TryContext와 헤더에서 tryId를 찾지 못했습니다. headers={}", accessor.toMap());
                return message;
            }
            Scope scope = TryContext.setTryId(tryId);
            if (scope != null) {
                accessor.setHeader(TryStompHeaders.INTERNAL_OUTBOUND_SCOPE_HEADER, scope);
            }
        }

        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        log.trace("Outbound STOMP 메시지에 tryId({}) 헤더를 설정했습니다.", tryId);

        StompHeaderAccessor newAccessor = StompHeaderAccessor.create(
                accessor.getCommand() != null ? accessor.getCommand() : StompCommand.MESSAGE
        );
        newAccessor.copyHeaders(accessor.getMessageHeaders());
        newAccessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
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
        Object scopeObject = accessor.getHeader(TryStompHeaders.INTERNAL_OUTBOUND_SCOPE_HEADER);
        if (scopeObject instanceof Scope scope) {
            try {
                scope.close();
            } catch (Exception e) {
                log.warn("Outbound Try Scope 정리 중 예외가 발생했습니다: {}", e.getMessage());
            }
        }

        if (!sent) {
            log.warn("STOMP 메시지 전송이 완료되지 않았습니다. simpSessionId={}, headers={}, exception={}", accessor.getSessionId(), accessor.toMap(), ex != null ? ex.getMessage() : "none");
        }
    }

    private UUID extractTryIdFromHeaders(StompHeaderAccessor accessor) {
        String headerTryId = accessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER);
        if (headerTryId != null) {
            try {
                return UUID.fromString(headerTryId);
            } catch (IllegalArgumentException e) {
                log.warn("Outbound STOMP 헤더의 tryId 값이 UUID 형식이 아닙니다: {}", headerTryId);
            }
        }

        return null;
    }
}


