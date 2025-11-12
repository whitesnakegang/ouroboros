package kr.co.ouroboros.ui.websocket.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating a new AsyncAPI message definition.
 * <p>
 * Used to define reusable message definitions in the AsyncAPI components/messages section.
 * Messages can be referenced by operations and channels for WebSocket communication.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequest {

    /**
     * Message name (identifier used for $ref references)
     * <p>
     * Example: "UserMessage", "ChatMessage", "NotificationMessage"
     */
    private String messageName;

    /**
     * Human-readable name for the message
     */
    private String name;

    /**
     * Content type for the message
     * <p>
     * Defaults to "application/json" if not provided
     */
    @Builder.Default
    private String contentType = "application/json";

    /**
     * Description of what this message represents
     */
    private String description;

    /**
     * Message headers definition (optional)
     * <p>
     * Defines the headers that can be sent with the message.
     * Can reference a schema using {"$ref": "HeaderSchemaName"} or define inline properties.
     */
    private Map<String, Object> headers;

    /**
     * Message payload definition
     * <p>
     * Can reference a schema using {"$ref": "SchemaName"} or define inline schema.
     * Example: {"$ref": "#/components/schemas/UserMessage"}
     */
    private Map<String, Object> payload;
}
