package kr.co.ouroboros.core.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {

    private String name;
    private String in; // "path", "query", "header", "cookie"
    private String description;
    private boolean required;
    private Schema schema;
}
