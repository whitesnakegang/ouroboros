package kr.co.ouroboros.core.websocket.spec.service;

import kr.co.ouroboros.ui.websocket.spec.dto.WebSocketStatusResponse;

/**
 * Service interface for checking WebSocket protocol status.
 *
 * @since 0.1.0
 */
public interface WebSocketStatusService {

    /**
     * Retrieves the current status of the WebSocket protocol handler.
     * <p>
     * Checks whether Springwolf-based code scanning is enabled or if the basic handler is active.
     *
     * @return status information including code scanning availability and handler type
     */
    WebSocketStatusResponse getStatus();
}
