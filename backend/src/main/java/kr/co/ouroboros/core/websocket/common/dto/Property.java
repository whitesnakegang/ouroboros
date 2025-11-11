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
    
    @JsonProperty("$ref")
    private String ref;
}

