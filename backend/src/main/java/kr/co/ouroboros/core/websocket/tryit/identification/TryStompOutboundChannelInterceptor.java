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

        // StompCommand가 없으면 MESSAGE로 설정 (StompEncoder에서 필수)
        // 브로커가 브로드캐스트 메시지를 만들 때 command를 설정하지 않는 경우가 있음
        StompCommand command = accessor.getCommand();
        boolean needNewAccessor = (command == null);
        
        if (needNewAccessor) {
            // command가 없으면 새 accessor를 생성하고 원본 메시지의 모든 헤더를 보존
            // 특히 Spring 메시징 헤더(simpSubscriptionId, simpDestination 등)를 명시적으로 복사
            StompHeaderAccessor newAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
            newAccessor.setLeaveMutable(true);
            
            // 원본 메시지의 모든 헤더를 복사 (Spring 메시징 헤더 포함)
            // copyHeaders()로 기본 헤더를 복사한 후, Spring 메시징 헤더를 명시적으로 설정
            newAccessor.copyHeaders(message.getHeaders());
            
            // 원본 accessor에서 중요한 Spring 메시징 헤더를 명시적으로 복사
            // 이렇게 하면 StompEncoder가 subscription, destination 등을 제대로 인코딩할 수 있음
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
            
            // 원본 메시지의 native 헤더도 복사 (content-type 등)
            accessor.toNativeHeaderMap().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    newAccessor.setNativeHeader(key, values.get(0));
                }
            });
            
            // tryId 헤더 추가
            newAccessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
            newAccessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
            
            log.trace("Outbound STOMP 메시지에 tryId({}) 헤더를 설정했습니다 (command 없음, 새 accessor 생성). subscriptionId={}, destination={}", 
                    tryId, newAccessor.getSubscriptionId(), newAccessor.getDestination());
            
            // 새 accessor의 헤더로 메시지 재생성
            // 원본 메시지의 payload는 그대로 유지하고 헤더만 업데이트
            return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
        }
        
        // command가 있으면 원본 accessor에 tryId 헤더만 추가
        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        
        log.trace("Outbound STOMP 메시지에 tryId({}) 헤더를 설정했습니다. command={}, destination={}, subscriptionId={}", 
                tryId, accessor.getCommand(), accessor.getDestination(), accessor.getSubscriptionId());

        // 원본 메시지의 모든 헤더를 보존하면서 tryId 헤더만 추가
        // accessor는 mutable 상태이므로 변경사항이 이미 반영되어 있음
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


