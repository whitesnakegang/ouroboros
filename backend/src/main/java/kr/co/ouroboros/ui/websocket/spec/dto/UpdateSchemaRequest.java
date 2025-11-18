package kr.co.ouroboros.ui.websocket.spec.dto;

import kr.co.ouroboros.core.websocket.spec.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing AsyncAPI schema definition.
 * <p>
 * Only provided fields will be updated. Null fields are ignored.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchemaRequest {

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
    private Object items;
}
