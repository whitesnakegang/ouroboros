package kr.co.ouroboros.ui.websocket.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.ouroboros.core.websocket.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for AsyncAPI schema operations.
 * <p>
 * Returns the complete schema definition including metadata.
 *
 * @since 0.1.0
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
     * Array item schema (for type="array")
     */
    private Property items;
}
