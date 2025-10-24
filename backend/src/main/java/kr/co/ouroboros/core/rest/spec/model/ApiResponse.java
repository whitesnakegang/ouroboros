package kr.co.ouroboros.core.rest.spec.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String name;
    private String description;
    private Map<String, MediaType> content;
    private Map<String, Header> headers;
}