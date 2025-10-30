package kr.co.ouroboros.core.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Schema {

    // Standard fields
    private String type;
    private String format;
    private String description;
    @JsonProperty("$ref")
    private String ref;

    // For object type
    private Map<String, Schema> properties; // (재귀 구조)
    private Xml xml;
    private List<String> required;

    // For array type
    private Schema items; // (재귀 구조)
    private Integer minItems;
    private Integer maxItems;

    @JsonProperty("additionalProperties")
    private Map<String, Object> additionalProperties;
    // (★핵심★) Ouroboros custom fields
    @JsonProperty("x-ouroboros-mock")
    private String xOuroborosMock;

    @JsonProperty("x-ouroboros-orders")
    private List<String> xOuroborosOrders;
}