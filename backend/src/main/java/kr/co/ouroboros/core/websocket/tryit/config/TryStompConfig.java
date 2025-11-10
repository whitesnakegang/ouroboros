package kr.co.ouroboros.core.websocket.tryit.config;

import kr.co.ouroboros.core.websocket.tryit.identification.TryStompChannelInterceptor;
import kr.co.ouroboros.core.websocket.tryit.identification.TryStompOutboundChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP Try 모듈 설정.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TryStompConfig implements WebSocketMessageBrokerConfigurer {

    private final TryStompChannelInterceptor tryStompChannelInterceptor;
    private final TryStompOutboundChannelInterceptor tryStompOutboundChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Registering TryStompChannelInterceptor for inbound channel");
        registration.interceptors(tryStompChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        log.info("Registering TryStompOutboundChannelInterceptor for outbound channel");
        registration.interceptors(tryStompOutboundChannelInterceptor);
    }

    /**
     * 빈 메서드지만, Spring Boot 자동 설정과 충돌을 피하기 위해 존재한다.
     */
    // 메시지 브로커 설정은 애플리케이션 기존 설정을 그대로 사용한다.
}


