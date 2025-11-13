package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Message Reference Object.
 * <p>
 * A reference to a message definition in the components section.
 * <p>
 * JSON API uses "ref" field, but YAML storage uses "$ref" (AsyncAPI standard).
 * Service layer handles conversion between "ref" (JSON) and "$ref" (YAML).
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageReference {

    /**
     * Message reference path.
     * <p>
     * In JSON API: "ref" field (e.g., "ChatMessage" or "#/components/messages/ChatMessage")
     * In YAML storage: "$ref" field (e.g., "#/components/messages/ChatMessage")
     */
    @JsonProperty("$ref")
    private String ref;
}

