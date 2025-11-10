package kr.co.ouroboros.core.websocket.tryit.identification;

import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

        UUID tryId = TryContext.getTryId();
        if (tryId == null) {
            return message;
        }

        accessor.setNativeHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        accessor.setHeader(TryStompHeaders.TRY_ID_HEADER, tryId.toString());
        log.trace("Outbound STOMP 메시지에 tryId({}) 헤더를 설정했습니다.", tryId);
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}


