package kr.co.ouroboros.ui.rest.spec.dto;

import kr.co.ouroboros.core.rest.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing schema definition.
 * <p>
 * All fields are optional - only provided fields will be updated.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchemaRequest {

    /**
     * Schema type (typically "object" for complex types)
     */
    private String type;

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
    
    /**
     * Array item schema (for type="array")
     */
    private Object items;
}
