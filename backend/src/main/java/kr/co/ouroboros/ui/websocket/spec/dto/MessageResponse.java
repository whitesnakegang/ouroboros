package kr.co.ouroboros.ui.websocket.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for AsyncAPI message operations.
 * <p>
 * Returns the complete message definition including metadata.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    /**
     * Message name (identifier)
     */
    private String messageName;

    /**
     * Human-readable name
     */
    private String name;

    /**
     * Content type
     */
    private String contentType;

    /**
     * Message description
     */
    private String description;

    /**
     * Message headers definition
     */
    private Map<String, Object> headers;

    /**
     * Message payload definition
     */
    private Map<String, Object> payload;
}
