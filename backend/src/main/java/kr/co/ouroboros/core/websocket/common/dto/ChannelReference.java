package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Channel Reference Object.
 * <p>
 * A reference to a channel definition.
 * <p>
 * JSON API uses "ref" field, but YAML storage uses "$ref" (AsyncAPI standard).
 * Service layer handles conversion between "ref" (JSON) and "$ref" (YAML).
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelReference {

    /**
     * Channel reference path.
     * <p>
     * In JSON API: "ref" field (e.g., "_chat.send" or "#/channels/_chat.send")
     * In YAML storage: "$ref" field (e.g., "#/channels/_chat.send")
     */
    @JsonProperty("$ref")
    private String ref;
}

