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
public class Operation {

    // Standard fields
    private String summary;
    private boolean deprecated;
    private String description;
    private List<String> tags;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, Response> responses;

    // (★핵심★) Ouroboros custom fields
    @JsonProperty("x-ouroboros-id")
    private String xOuroborosId;

    @JsonProperty("x-ouroboros-progress")
    private String xOuroborosProgress;

    @JsonProperty("x-ouroboros-tag")
    private String xOuroborosTag;

    @JsonProperty("x-ouroboros-diff")
    private String xOuroborosDiff;
}