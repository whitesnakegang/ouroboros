package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
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
     * Used internally by convertSpecToMap for YAML operations.
     * Exposed in JSON response via OperationResponse.id (not directly from Operation).
     */
    @JsonProperty("x-ouroboros-id")
    private String xOuroborosId;

    /**
     * Ouroboros custom field: WebSocket entry point (pathname) for this operation.
     * <p>
     * Stored in YAML as x-ouroboros-entrypoint.
     * Used internally by convertSpecToMap for YAML operations.
     * Exposed in JSON response via OperationResponse.entrypoint (not directly from Operation).
     * Example: "/ws", "/stomp/v1"
     */
    @JsonProperty("x-ouroboros-entrypoint")
    private String xOuroborosEntrypoint;

    /**
     * Ouroboros custom field: specification drift detection status.
     * <p>
     * Stored in YAML as x-ouroboros-diff.
     * Used internally by convertSpecToMap for YAML operations.
     * Exposed in JSON response via OperationResponse.diff (not directly from Operation).
     * Possible values: "none", "payload", "channel"
     */
    @JsonProperty("x-ouroboros-diff")
    private String xOuroborosDiff;

    /**
     * Ouroboros custom field: development progress status.
     * <p>
     * Stored in YAML as x-ouroboros-progress.
     * Used internally by convertSpecToMap for YAML operations.
     * Exposed in JSON response via OperationResponse.progress (not directly from Operation).
     * Possible values: "none", "mock", "completed"
     */
    @JsonProperty("x-ouroboros-progress")
    private String xOuroborosProgress;


   }

