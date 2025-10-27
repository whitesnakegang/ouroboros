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
}