package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating WebSocket operations.
 * <p>
 * Updates operation configuration. Uses the same structure as CreateOperationRequest
 * for consistency. Action is automatically determined:
 * <ul>
 *   <li>If only reply is provided: action = "send"</li>
 *   <li>If receive is provided (with or without reply): action = "receive"</li>
 * </ul>
 * Only provided fields will be updated.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOperationRequest {

    /**
     * WebSocket protocol (ws or wss).
     * <p>
     * If provided, updates the server entry point.
     * Example: "ws", "wss"
     */
    private String protocol;

    /**
     * WebSocket pathname (entry point).
     * <p>
     * If provided, updates the server entry point.
     * Example: "/ws", "/websocket"
     */
    private String pathname;

    /**
     * Updated receive channel configuration.
     * <p>
     * Uses ChannelMessageInfo to support both address (auto-create) and channelRef (existing).
     * If provided, replaces the existing receive channel.
     * When receive is provided, action is automatically set to "receive".
     */
    private ChannelMessageInfo receive;

    /**
     * Updated reply channel configuration.
     * <p>
     * Uses ChannelMessageInfo to support both address (auto-create) and channelRef (existing).
     * If provided, replaces the existing reply channel.
     * If only reply is provided (no receive), action is automatically set to "send".
     */
    private ChannelMessageInfo reply;
}

