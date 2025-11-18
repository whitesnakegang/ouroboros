package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for WebSocket status information.
 * <p>
 * Provides information about whether Springwolf-based code scanning is active.
 *
 * @since 0.1.0
 */
@Data
@Builder
public class WebSocketStatusResponse {

    /**
     * Indicates whether Springwolf-based code scanning is active.
     * <ul>
     *   <li>{@code true} - Springwolf is enabled, code scanning is available</li>
     *   <li>{@code false} - Springwolf is disabled, only manual CRUD is available</li>
     * </ul>
     */
    private boolean active;
}