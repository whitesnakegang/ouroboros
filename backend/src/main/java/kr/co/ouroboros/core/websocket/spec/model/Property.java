package kr.co.ouroboros.core.websocket.spec.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a property within an AsyncAPI schema.
 * <p>
 * Defines individual fields in a data object including type and description.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><strong>Reference mode:</strong> Uses {@code ref} (simplified) or {@code $ref} (AsyncAPI standard) to reference another schema</li>
 *   <li><strong>Inline mode:</strong> Defines property structure inline with type, description, etc.</li>
 * </ul>
 * <p>
 * For array types, the items field specifies the property definition of array elements,
 * and minItems/maxItems define the valid range of array length.
 * This is a recursive structure allowing nested arrays, complex types, and schema references.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property {
    /**
     * Schema reference by name (simplified format).
     * <p>
     * Client sends just the schema name (e.g., "User") and the server
     * automatically converts it to the full AsyncAPI reference format when writing to YAML.
     * <p>
     * Example input: {@code "User"}
     * <p>
     * Stored in YAML as: {@code "$ref": "#/components/schemas/User"}
     */
    private String ref;

    private String type;
    private String description;

    // For object types - nested properties (recursive structure)
    private java.util.Map<String, Property> properties;
    private java.util.List<String> required;

    // For array types - recursive structure
    private Property items;
    private Integer minItems;  // Minimum array length for mock generation (default: 1)
    private Integer maxItems;  // Maximum array length for mock generation (default: 3)

    // Format (for distinguishing special types like "date-time", "uuid", etc.)
    private String format;
}