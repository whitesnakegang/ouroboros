package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Channel and message information for operation creation.
 * <p>
 * Either address (for new channel) or channelRef (for existing channel) must be provided.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMessageInfo {

    /**
     * Channel address (for creating new channel)
     * <p>
     * Example: "/chat.send", "/topic/rooms/public"
     * <p>
     * Mutually exclusive with channelRef.
     */
    private String address;

    /**
     * Existing channel reference (for using existing channel)
     * <p>
     * Example: "_chat.send"
     * <p>
     * Mutually exclusive with address.
     */
    private String channelRef;

    /**
     * List of message names to associate with this channel
     * <p>
     * Example: ["ChatMessage", "SystemMessage"]
     */
    private List<String> messages;
}