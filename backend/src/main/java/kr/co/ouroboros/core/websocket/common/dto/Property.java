package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Schema Property Object.
 * <p>
 * Represents a property definition within a schema.
 * <p>
 * JSON API uses "ref" field, but YAML storage uses "$ref" (AsyncAPI standard).
 * Service layer handles conversion between "ref" (JSON) and "$ref" (YAML).
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Property {

    private String type;
    
    @JsonProperty("enum")
    private List<String> enumValues;
    
    /**
     * Schema reference path.
     * <p>
     * In JSON API: "ref" field (e.g., "User" or "#/components/schemas/User")
     * In YAML storage: "$ref" field (e.g., "#/components/schemas/User")
     */
    @JsonProperty("ref")
    private String ref;
}

