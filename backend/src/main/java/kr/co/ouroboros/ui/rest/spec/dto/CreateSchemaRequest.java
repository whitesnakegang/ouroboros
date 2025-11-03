package kr.co.ouroboros.ui.rest.spec.dto;

import kr.co.ouroboros.core.rest.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new schema definition.
 * <p>
 * Used to define reusable data schemas in the OpenAPI components/schemas section.
 * Schemas can be referenced by API operations for request/response body definitions.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemaRequest {

    /**
     * Schema name (identifier used for $ref references)
     * <p>
     * Example: "User", "Book", "ErrorResponse"
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
     * XML root element name (used when content-type is application/xml)
     */
    private String xmlName;
}
