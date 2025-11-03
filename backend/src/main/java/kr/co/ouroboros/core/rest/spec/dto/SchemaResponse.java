package kr.co.ouroboros.core.rest.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.ouroboros.core.rest.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for schema operations.
 * <p>
 * Returns the complete schema definition including metadata.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaResponse {

    /**
     * Schema name (identifier)
     */
    private String schemaName;

    /**
     * Schema type
     */
    private String type;

    /**
     * Human-readable title
     */
    private String title;

    /**
     * Schema description
     */
    private String description;

    /**
     * Property definitions
     */
    private Map<String, Property> properties;

    /**
     * Required property names
     */
    private List<String> required;

    /**
     * Field ordering (Ouroboros extension)
     */
    private List<String> orders;

    /**
     * XML root element name
     */
    private String xmlName;
}
