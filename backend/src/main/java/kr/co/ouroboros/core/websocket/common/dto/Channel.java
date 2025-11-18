package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Channel Object.
 * <p>
 * Represents a communication channel in the WebSocket API.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Channel {

    private String address;
    private Map<String, MessageReference> messages;
    private Map<String, Object> bindings;
}

