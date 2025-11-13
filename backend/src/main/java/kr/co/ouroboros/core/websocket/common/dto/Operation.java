package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
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
     * Exposed in JSON as "id" (without x-ouroboros- prefix).
     */
    @JsonProperty(value = "x-ouroboros-id", access = JsonProperty.Access.WRITE_ONLY)
    private String xOuroborosId;

    /**
     * Ouroboros custom field: WebSocket entry point (pathname) for this operation.
     * <p>
     * Stored in YAML as x-ouroboros-entrypoint.
     * Exposed in JSON as "entrypoint" (without x-ouroboros- prefix).
     * Used to identify which server this operation uses.
     * Example: "/ws", "/stomp/v1"
     */
    @JsonProperty(value = "x-ouroboros-entrypoint", access = JsonProperty.Access.WRITE_ONLY)
    private String xOuroborosEntrypoint;

    /**
     * Ouroboros custom field: specification drift detection status.
     * <p>
     * Stored in YAML as x-ouroboros-diff.
     * Exposed in JSON as "diff" (without x-ouroboros- prefix).
     * Possible values: "none", "payload", "channel"
     */
    @JsonProperty(value = "x-ouroboros-diff", access = JsonProperty.Access.WRITE_ONLY)
    private String xOuroborosDiff;

    /**
     * Ouroboros custom field: development progress status.
     * <p>
     * Stored in YAML as x-ouroboros-progress.
     * Exposed in JSON as "progress" (without x-ouroboros- prefix).
     * Possible values: "none", "mock", "completed"
     */
    @JsonProperty(value = "x-ouroboros-progress", access = JsonProperty.Access.WRITE_ONLY)
    private String xOuroborosProgress;

   }

