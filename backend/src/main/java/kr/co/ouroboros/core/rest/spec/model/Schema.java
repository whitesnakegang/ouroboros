package kr.co.ouroboros.core.rest.spec.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a data schema definition.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><strong>Reference mode:</strong> Uses {@code ref} (simplified) or {@code $ref} (OpenAPI standard) to reference a schema from components/schemas</li>
 *   <li><strong>Inline mode:</strong> Defines schema structure inline with type, properties, etc.</li>
 * </ul>
 * <p>
 * When {@code $ref} is present, other fields are ignored according to OpenAPI specification.
 * Conforms to OpenAPI 3.1.0 Schema Object (JSON Schema compatible).
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schema {

    /**
     * Schema reference by name (simplified format).
     * <p>
     * Client sends just the schema name (e.g., "User") and the server
     * automatically converts it to the full OpenAPI reference format when writing to YAML.
     * <p>
     * Example input: {@code "User"}
     * <p>
     * Stored in YAML as: {@code "$ref": "#/components/schemas/User"}
     */
    private String ref;

    /**
     * Schema type (object, string, integer, etc.)
     * <p>
     * Used in inline mode only.
     */
    private String type;

    /**
     * Human-readable schema title
     * <p>
     * Used in inline mode only.
     */
    private String title;

    /**
     * Schema description
     * <p>
     * Used in inline mode only.
     */
    private String description;

    /**
     * Property definitions for object type schemas
     * <p>
     * Used in inline mode only.
     */
    private Map<String, Property> properties;

    /**
     * List of required property names
     * <p>
     * Used in inline mode only.
     */
    private List<String> required;

    /**
     * Custom field ordering (Ouroboros extension)
     * <p>
     * Used in inline mode only.
     */
    private List<String> orders;

    /**
     * XML root element name
     * <p>
     * Used in inline mode only for XML content types.
     */
    private String xmlName;
}