package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestApiSpec {
    private String path;
    private String method;
    private String summary;
    private String description;
    private boolean deprecated;
    private List<String> tags;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, ApiResponse> responses;
    private List<SecurityRequirement> security;
}
