package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Message Reference Object.
 * <p>
 * A reference to a message definition in the components section.
 * <p>
 * JSON API uses "$ref" field (AsyncAPI standard).
 * Accepts both "$ref" and "ref" during deserialization for compatibility.
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
     * JSON field: "$ref" (e.g., "#/components/messages/ChatMessage")
     * Also accepts "ref" for internal conversion compatibility
     */
    @JsonProperty("$ref")
    @JsonAlias("ref")
    private String ref;
}

