package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.WebSocketStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link WebSocketStatusService}.
 * <p>
 * Checks the active WebSocket handler bean to determine if code scanning is available.
 *
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketStatusServiceImpl implements WebSocketStatusService {

    private final ApplicationContext applicationContext;

    /**
     * Retrieves the current status of the WebSocket protocol handler.
     * <p>
     * Checks whether the Springwolf-based handler (bean name: "ouroWebSocketHandler") is active.
     * If present, code scanning is enabled. Otherwise, the basic handler is active.
     *
     * @return status information with active flag indicating code scanning availability
     */
    @Override
    public WebSocketStatusResponse getStatus() {
        // Check if Springwolf-based handler is active
        boolean hasSpringwolfHandler = applicationContext.containsBean("ouroWebSocketHandler");

        if (hasSpringwolfHandler) {
            log.debug("Springwolf-based WebSocket handler is active");
            return WebSocketStatusResponse.builder()
                    .active(true)
                    .build();
        } else {
            log.debug("Basic WebSocket handler is active (Springwolf disabled)");
            return WebSocketStatusResponse.builder()
                    .active(false)
                    .build();
        }
    }
}
