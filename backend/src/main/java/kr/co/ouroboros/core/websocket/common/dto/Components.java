package kr.co.ouroboros.core.websocket.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AsyncAPI Components Object.
 * <p>
 * Contains reusable components including schemas and messages.
 *
 * @since 1.0.0
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
     * Reusable message definitions referenced via $ref.
     */
    private Map<String, Message> messages;
}

