package kr.co.ouroboros.ui.websocket.spec.dto;

import kr.co.ouroboros.core.websocket.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new AsyncAPI schema definition.
 * <p>
 * Used to define reusable data schemas in the AsyncAPI components/schemas section.
 * Schemas can be referenced by message payloads for WebSocket communication.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemaRequest {

    /**
     * Schema name (identifier used for $ref references)
     * <p>
     * Example: "User", "ChatMessage", "ErrorResponse"
     */
    private String schemaName;

    /**
     * Schema type (typically "object" for complex types)
     * <p>
     * Defaults to "object" if not provided
     */
    @Builder.Default
    private String type = "object";

    /**
     * Human-readable title for the schema
     */
    private String title;

    /**
     * Description of what this schema represents
     */
    private String description;

    /**
     * Property definitions for object type schemas
     * <p>
     * Key: property name, Value: property definition
     */
    private Map<String, Property> properties;

    /**
     * List of required property names
     */
    private List<String> required;

    /**
     * Custom field ordering for property display (Ouroboros extension)
     */
    private List<String> orders;

    /**
     * Array item schema (for type="array")
     * <p>
     * Defines the structure of each element in the array
     */
    private Object items;
}
