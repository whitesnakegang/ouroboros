package kr.co.ouroboros.core.websocket.tryit.identification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import kr.co.ouroboros.core.websocket.tryit.common.TryStompHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TryPublisherNotifierTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider = mock(ObjectProvider.class);
    private final TrySessionRegistry sessionRegistry = mock(TrySessionRegistry.class);
    private final TryPublisherNotifier notifier = new TryPublisherNotifier(messagingTemplateProvider, sessionRegistry);

    @Test
    void notifyPublisher_shouldRegisterAndSendMessage() {
        UUID tryId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-123");
        accessor.setLeaveMutable(true);
        accessor.setDestination("/app/try");
        accessor.setNativeHeader("sample-header", "value");

        Message<byte[]> message = MessageBuilder.createMessage("{}".getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());

        when(messagingTemplateProvider.getIfAvailable()).thenReturn(messagingTemplate);

        notifier.notifyPublisher(tryId, accessor, message);

        ArgumentCaptor<TrySessionRegistry.TrySessionRegistration> registrationCaptor =
                ArgumentCaptor.forClass(TrySessionRegistry.TrySessionRegistration.class);

        verify(sessionRegistry).register(eq(tryId), registrationCaptor.capture());
        TrySessionRegistry.TrySessionRegistration registration = registrationCaptor.getValue();
        assertThat(registration.sessionId()).isEqualTo("session-123");
        TryDispatchMessage dispatchMessage = registration.message();
        assertThat(dispatchMessage.headers()).containsEntry("sample-header", "value");
        assertThat(dispatchMessage.headers()).containsEntry("destination", "/app/try");
        assertThat(dispatchMessage.headers()).doesNotContainKey(TryStompHeaders.TRY_ID_HEADER);
        assertThat(dispatchMessage.payload()).isEqualTo("{}");

        verify(messagingTemplate).convertAndSendToUser(eq("session-123"), eq("/queue/try"), eq(dispatchMessage), anyMap());
    }
}

