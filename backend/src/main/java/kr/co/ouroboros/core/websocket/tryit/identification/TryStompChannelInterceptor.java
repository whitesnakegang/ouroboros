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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * STOMP 인바운드 메시지에서 Try 요청을 식별하고 Try 컨텍스트를 설정하는 인터셉터.
 * <p>
 * HTTP {@link kr.co.ouroboros.core.rest.tryit.identification.TryFilter}와 동일한 헤더 프로토콜을 사용하여
 * STOMP 메시지에서도 tryId를 발급 및 전파한다.
 */
@Slf4j
@Component
public class TryStompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();
        if (command == null) {
            log.trace("STOMP 명령이 없어 Try 검사를 건너뜁니다.");
            return message;
        }

        // Inbound 채널(클라이언트 -> 서버) 메시지만 처리된다. command 기준으로 추가 필터링 가능.
        String tryHeader = getFirstNativeHeader(accessor, TryStompHeaders.TRY_HEADER);
        boolean tryRequested = TryStompHeaders.TRY_HEADER_ENABLED_VALUE.equalsIgnoreCase(tryHeader);

        UUID tryId = resolveTryId(accessor, tryRequested);
        if (tryId == null) {
            // Try 요청이 아니거나, Try 식별자를 발급하지 못한 경우 컨텍스트를 건드리지 않는다.
            return message;
        }

        boolean mutated = false;

        Scope scope = TryContext.setTryId(tryId);
        if (scope != null) {
            accessor.setHeader(TryStompHeaders.INTERNAL_SCOPE_HEADER, scope);
            mutated = true;
        }

        // 세션과 헤더에 tryId를 기록하여 이후 프레임에서도 재사용한다.
        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        mutated = true;
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put(TryStompHeaders.SESSION_TRY_ID_ATTR, tryId.toString());
        } else {
            Map<String, Object> newSessionAttributes = new HashMap<>();
            newSessionAttributes.put(TryStompHeaders.SESSION_TRY_ID_ATTR, tryId.toString());
            accessor.setSessionAttributes(newSessionAttributes);
            mutated = true;
        }

        log.trace("STOMP {} 프레임에서 Try 컨텍스트를 설정했습니다. tryId={}", command, tryId);
        if (!mutated) {
            return message;
        }

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

        Scope scope = (Scope) accessor.getHeader(TryStompHeaders.INTERNAL_SCOPE_HEADER);
        if (scope != null) {
            try {
                scope.close();
            } catch (Exception e) {
                log.warn("Try STOMP Scope 정리 중 예외가 발생했습니다: {}", e.getMessage());
            }
        }
    }

    @Nullable
    private UUID resolveTryId(StompHeaderAccessor accessor, boolean tryRequested) {
        // 1) 이미 세션에 tryId가 저장되어 있다면 재사용
        UUID sessionTryId = extractSessionTryId(accessor);
        if (sessionTryId != null) {
            return sessionTryId;
        }

        // 2) 프레임 헤더에 tryId가 포함되어 있다면 그대로 사용
        String headerTryId = getFirstNativeHeader(accessor, TryStompHeaders.TRY_ID_HEADER);
        if (headerTryId != null) {
            try {
                return UUID.fromString(headerTryId);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 tryId 형식이 STOMP 헤더에 전달되었습니다: {}", headerTryId);
            }
        }

        // 3) Try 요청 헤더가 활성화됐으면 새로 생성
        if (tryRequested) {
            UUID newTryId = UUID.randomUUID();
            log.debug("STOMP Try 요청을 감지하여 새로운 tryId를 생성했습니다: {}", newTryId);
            return newTryId;
        }

        return null;
    }

    @Nullable
    private UUID extractSessionTryId(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object sessionValue = sessionAttributes.get(TryStompHeaders.SESSION_TRY_ID_ATTR);
        if (sessionValue == null) {
            return null;
        }
        try {
            return UUID.fromString(Objects.toString(sessionValue));
        } catch (IllegalArgumentException e) {
            log.warn("세션에 저장된 tryId 형식이 올바르지 않습니다: {}", sessionValue);
            return null;
        }
    }

    @Nullable
    private String getFirstNativeHeader(StompHeaderAccessor accessor, String headerName) {
        if (accessor.getNativeHeader(headerName) == null || accessor.getNativeHeader(headerName).isEmpty()) {
            return null;
        }
        return accessor.getFirstNativeHeader(headerName);
    }
}


