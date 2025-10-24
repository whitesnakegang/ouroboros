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
public class Schema {
    private String type;
    private String title;
    private String description;
    private Map<String, Property> properties;
    private List<String> required;
    private List<String> orders;
}