package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Map<String, Object> bindings;
    private List<MessageReference> messages;
    private Reply reply;

    /**
     * Ouroboros custom field: unique identifier for the operation.
     * <p>
     * Stored in YAML as x-ouroboros-id.
     */
    @JsonProperty("x-ouroboros-id")
    private String xOuroborosId;

    /**
     * Ouroboros custom field: WebSocket entry point (pathname) for this operation.
     * <p>
     * Stored in YAML as x-ouroboros-entrypoint.
     * Used to identify which server this operation uses.
     * Example: "/ws", "/stomp/v1"
     */
    @JsonProperty("x-ouroboros-entrypoint")
    private String xOuroborosEntrypoint;

    /**
     * Ouroboros custom field: specification drift detection status.
     * <p>
     * Stored in YAML as x-ouroboros-diff.
     * Possible values: "none", "payload", "channel"
     */
    @JsonProperty("x-ouroboros-diff")
    private String xOuroborosDiff;

    /**
     * Ouroboros custom field: development progress status.
     * <p>
     * Stored in YAML as x-ouroboros-progress.
     * Possible values: "none", "mock", "completed"
     */
    @JsonProperty("x-ouroboros-progress")
    private String xOuroborosProgress;
}

