package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Message Object.
 * <p>
 * Represents a message definition in the WebSocket API.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private String name;
    private String title;
    private MessageHeaders headers;
    private Payload payload;
    private Map<String, Object> bindings;
}

