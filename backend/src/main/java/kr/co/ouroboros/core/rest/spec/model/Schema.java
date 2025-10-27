package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a data schema definition.
 * <p>
 * Defines the structure and validation rules for data objects in request/response bodies.
 * Includes type information, property definitions, and field ordering.
 * Conforms to OpenAPI 3.1.0 Schema Object (JSON Schema compatible).
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schema {
    private String type;
    private String title;
    private String description;
    private Map<String, Property> properties;
    private List<String> required;
    private List<String> orders;
}