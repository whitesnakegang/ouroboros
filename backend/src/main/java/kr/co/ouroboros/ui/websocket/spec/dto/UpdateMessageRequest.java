package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating an existing AsyncAPI message definition.
 * <p>
 * Only provided fields will be updated. Null fields are ignored.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMessageRequest {

    /**
     * Human-readable name for the message
     */
    private String name;

    /**
     * Content type for the message
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
