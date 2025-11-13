package kr.co.ouroboros.core.websocket.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers the STOMP WebSocket endpoint used by clients.
     *
     * Registers "/ws" as the WebSocket/STOMP endpoint, permits all origin patterns ("*") and enables SockJS fallback support.
     *
     * @param registry the StompEndpointRegistry used to add and configure STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")              // ws://localhost:8080/ws
                .setAllowedOriginPatterns("*")   // 데모용 전체 허용 (운영은 꼭 제한!)
                .withSockJS();                   // 프론트에서 SockJS 사용 시
    }

    /**
     * Configure STOMP broker destinations for server-to-client broadcasts and the application
     * prefix used for routing client messages to @MessageMapping handlers.
     *
     * @param registry the MessageBrokerRegistry used to enable broker destinations and set application prefixes
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트로 브로드캐스트할 때 쓰는 prefix (구독 대상)
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버로 보낼 때 쓰는 prefix (@MessageMapping 라우팅)
        registry.setApplicationDestinationPrefixes("/app");
    }
}