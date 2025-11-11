package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Operation Object.
 * <p>
 * Represents an operation (send or receive) on a channel.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Operation {

    private String action;
    private ChannelReference channel;
    private List<Map<String, Object>> tags;
    private Map<String, Object> bindings;
    private List<MessageReference> messages;
    private Reply reply;
}

