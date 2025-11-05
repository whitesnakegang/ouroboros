package kr.co.ouroboros.core.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI Components Object.
 * <p>
 * Contains reusable components including schemas and security schemes.
 *
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Components {

    /**
     * Reusable schema definitions referenced via $ref.
     */
    private Map<String, Schema> schemas;
    
    /**
     * Reusable security scheme definitions.
     * <p>
     * Each key is a security scheme name (e.g., "BearerAuth", "ApiKeyAuth"),
     * and the value is the security scheme definition containing type, scheme, etc.
     */
    private Map<String, Object> securitySchemes;
}
