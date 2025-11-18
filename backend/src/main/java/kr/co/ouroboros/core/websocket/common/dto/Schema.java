package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Schema Object.
 * <p>
 * Represents a JSON Schema definition for message payloads.
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schema {

    private String title;
    private String type;
    private String format;
    private Map<String, Schema> properties;

    @JsonProperty("enum")
    private List<String> enumValues;
    
    @JsonProperty("$ref")
    private String ref;
    
    private Schema items;
    private List<String> required;
    private List<Object> examples;
}

