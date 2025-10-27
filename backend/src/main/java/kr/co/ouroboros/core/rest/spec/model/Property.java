package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a property within a schema.
 * <p>
 * Defines individual fields in a data object including type, description,
 * and optional mock data generation expressions.
 * For array types, the items field specifies the property definition of array elements,
 * and minItems/maxItems define the valid range of array length.
 * This is a recursive structure allowing nested arrays and complex types.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    private String type;
    private String description;
    private String mockExpression;
    private Property items;  // For array types - recursive structure
    private Integer minItems;  // Minimum number of items in array
    private Integer maxItems;  // Maximum number of items in array
}