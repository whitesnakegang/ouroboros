package kr.co.ouroboros.core.websocket.tryit.identification;

import io.opentelemetry.context.Scope;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TryStompChannelInterceptorTest {

    private final TryStompChannelInterceptor interceptor = new TryStompChannelInterceptor();
    private final TryStompOutboundChannelInterceptor outboundInterceptor = new TryStompOutboundChannelInterceptor();
    private final MessageChannel dummyChannel = new MessageChannel() {
        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    };

    @Test
    void preSend_shouldGenerateTryIdWhenHeaderOn() {
        Message<byte[]> message = createStompMessageWithHeader(StompCommand.SEND, Map.of(
                TryStompHeaders.TRY_HEADER, TryStompHeaders.TRY_HEADER_ENABLED_VALUE
        ));

        Message<?> intercepted = interceptor.preSend(message, dummyChannel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(intercepted, StompHeaderAccessor.class);
        assertNotNull(accessor);

        String tryIdHeader = accessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER);
        assertNotNull(tryIdHeader);
        assertDoesNotThrow(() -> UUID.fromString(tryIdHeader));
        assertEquals(tryIdHeader, accessor.getSessionAttributes().get(TryStompHeaders.SESSION_TRY_ID_ATTR));

        // 컨텍스트가 설정되었는지 확인
        UUID tryIdFromContext = TryContext.getTryId();
        assertNotNull(tryIdFromContext);
        assertEquals(tryIdHeader, tryIdFromContext.toString());

        interceptor.afterSendCompletion(intercepted, dummyChannel, true, null);
        assertNull(TryContext.getTryId());
    }

    @Test
    void preSend_shouldReuseSessionTryId() {
        UUID existingTryId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        accessor.getSessionAttributes().put(TryStompHeaders.SESSION_TRY_ID_ATTR, existingTryId.toString());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> intercepted = interceptor.preSend(message, dummyChannel);
        StompHeaderAccessor mutatedAccessor = MessageHeaderAccessor.getAccessor(intercepted, StompHeaderAccessor.class);
        assertNotNull(mutatedAccessor);

        String tryIdHeader = mutatedAccessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER);
        assertEquals(existingTryId.toString(), tryIdHeader);

        interceptor.afterSendCompletion(intercepted, dummyChannel, true, null);
        assertNull(TryContext.getTryId());
    }

    @Test
    void preSend_shouldIgnoreWhenTryHeaderAbsent() {
        Message<byte[]> message = createStompMessageWithHeader(StompCommand.SUBSCRIBE, Map.of());
        Message<?> intercepted = interceptor.preSend(message, dummyChannel);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(intercepted, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNull(accessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER));

        interceptor.afterSendCompletion(intercepted, dummyChannel, true, null);
        assertNull(TryContext.getTryId());
    }

    @Test
    void outboundInterceptor_shouldAttachTryIdFromContext() {
        UUID tryId = UUID.randomUUID();
        try (Scope scope = TryContext.setTryId(tryId)) {
            Message<byte[]> message = createStompMessageWithHeader(StompCommand.MESSAGE, Map.of());
            Message<?> intercepted = outboundInterceptor.preSend(message, dummyChannel);
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(intercepted, StompHeaderAccessor.class);
            assertNotNull(accessor);
            String tryIdHeader = accessor.getFirstNativeHeader(TryStompHeaders.TRY_ID_HEADER);
            assertEquals(tryId.toString(), tryIdHeader);
        }
    }

    private Message<byte[]> createStompMessageWithHeader(StompCommand command, Map<String, String> headers) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-try");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        headers.forEach(accessor::setNativeHeader);
        return MessageBuilder.createMessage("{}".getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
    }
}


