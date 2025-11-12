package kr.co.ouroboros.core.websocket.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 클라이언트가 실제로 접속하는 엔드포인트 (SockJS fallback 포함)
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")              // ws://localhost:8080/ws
                .setAllowedOriginPatterns("*")   // 데모용 전체 허용 (운영은 꼭 제한!)
                .withSockJS();                   // 프론트에서 SockJS 사용 시
    }

    // STOMP 목적지 프리픽스 구성
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트로 브로드캐스트할 때 쓰는 prefix (구독 대상)
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버로 보낼 때 쓰는 prefix (@MessageMapping 라우팅)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
