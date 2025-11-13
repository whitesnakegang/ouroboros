package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Message Headers Object.
 * <p>
 * Represents headers for a message.
 * <p>
 * JSON API uses "ref" field, but YAML storage uses "$ref" (AsyncAPI standard).
 * Service layer handles conversion between "ref" (JSON) and "$ref" (YAML).
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageHeaders {

    /**
     * Headers schema reference path.
     * <p>
     * In JSON API: "ref" field (e.g., "MessageHeaders" or "#/components/schemas/MessageHeaders")
     * In YAML storage: "$ref" field (e.g., "#/components/schemas/MessageHeaders")
     */
    @JsonProperty("ref")
    private String ref;
}

