package kr.co.ouroboros.ui.websocket.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item that was renamed during YAML import due to duplicates.
 * <p>
 * Can represent a channel, operation, schema, or message component.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenamedItem {
    /**
     * Type of the renamed item.
     * Valid values: "channel", "operation", "schema", "message"
     */
    private String type;

    /**
     * Original name before renaming
     */
    private String original;

    /**
     * New name after renaming (e.g., "UserMessage" → "UserMessage-import")
     */
    private String renamed;

    /**
     * Action type for operations (send/receive).
     * Only present when type is "operation".
     */
    private String action;
}