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

    @JsonProperty("x-ouroboros-id")
    private String xOuroborosId;

    @JsonProperty("x-ouroboros-progress")
    private String xOuroborosProgress;

    @JsonProperty("x-ouroboros-diff")
    private String xOuroborosDiff;
}

