package kr.co.ouroboros.core.rest.spec.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a property within a schema.
 * <p>
 * Defines individual fields in a data object including type, description,
 * and optional mock data generation expressions.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><strong>Reference mode:</strong> Uses {@code ref} (simplified) or {@code $ref} (OpenAPI standard) to reference another schema</li>
 *   <li><strong>Inline mode:</strong> Defines property structure inline with type, description, etc.</li>
 * </ul>
 * <p>
 * For array types, the items field specifies the property definition of array elements,
 * and minItems/maxItems define the valid range of array length.
 * This is a recursive structure allowing nested arrays, complex types, and schema references.
 *
 * @since 0.0.1
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
     * automatically converts it to the full OpenAPI reference format when writing to YAML.
     * <p>
     * Example input: {@code "User"}
     * <p>
     * Stored in YAML as: {@code "$ref": "#/components/schemas/User"}
     */
    private String ref;

    private String type;
    private String description;
    private String mockExpression;
    private Property items;  // For array types - recursive structure
    private Integer minItems;  // Minimum number of items in array
    private Integer maxItems;  // Maximum number of items in array
}