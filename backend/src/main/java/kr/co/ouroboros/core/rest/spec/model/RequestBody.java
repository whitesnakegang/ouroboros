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
public class RequestBody {
    private String description;
    private boolean required;
    private Map<String, MediaType> content;
}