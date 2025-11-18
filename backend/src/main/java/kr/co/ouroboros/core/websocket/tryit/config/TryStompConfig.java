package kr.co.ouroboros.core.websocket.tryit.config;

import kr.co.ouroboros.core.websocket.tryit.identification.TryStompChannelInterceptor;
import kr.co.ouroboros.core.websocket.tryit.identification.TryStompOutboundChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for STOMP Try module.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TryStompConfig implements WebSocketMessageBrokerConfigurer {

    private final TryStompChannelInterceptor tryStompChannelInterceptor;
    private final TryStompOutboundChannelInterceptor tryStompOutboundChannelInterceptor;

    /**
     * Registers the Try module's inbound STOMP channel interceptor so inbound messages are processed by it.
     *
     * @param registration the inbound channel registration to attach the interceptor to
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Registering TryStompChannelInterceptor for inbound channel");
        registration.interceptors(tryStompChannelInterceptor);
    }

    /**
     * Registers the TryStompOutboundChannelInterceptor on the provided client outbound channel registration.
     *
     * @param registration the outbound channel registration to which the interceptor will be added
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        log.info("Registering TryStompOutboundChannelInterceptor for outbound channel");
        registration.interceptors(tryStompOutboundChannelInterceptor);
    }

    /**
     * Empty method, but exists to avoid conflicts with Spring Boot auto-configuration.
     */
    // Message broker configuration uses the application's existing settings.
}

