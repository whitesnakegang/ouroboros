package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating WebSocket operations.
 * <p>
 * Creates operations that receive messages from receive channels and reply to reply channels.
 * Automatically generates channels and server entry point if they don't exist.
 * Generates all combinations of receive × reply operations.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOperationRequest {

    /**
     * WebSocket protocol (ws or wss).
     * <p>
     * Example: "ws", "wss"
     */
    private String protocol;

    /**
     * WebSocket pathname (entry point).
     * <p>
     * Example: "/ws", "/websocket"
     */
    private String pathname;

    /**
     * List of receive channel configurations.
     * <p>
     * Each entry defines a channel that receives messages (action: "receive").
     * Channels are auto-created if they don't exist.
     */
    private List<ChannelMessageInfo> receives;

    /**
     * List of reply channel configurations.
     * <p>
     * Each entry defines a channel that replies to received messages (action: "send").
     * Channels are auto-created if they don't exist.
     */
    private List<ChannelMessageInfo> replies;

    /**
     * Tags for categorizing and grouping operations.
     * <p>
     * Tag names will be automatically converted to uppercase on storage.
     * Example: ["user", "chat"] will be stored as ["USER", "CHAT"]
     */
    private List<String> tags;
}

