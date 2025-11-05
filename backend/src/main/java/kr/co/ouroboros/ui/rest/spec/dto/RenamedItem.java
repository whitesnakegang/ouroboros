package kr.co.ouroboros.ui.rest.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item that was renamed during YAML import due to duplicates.
 * <p>
 * Can represent either an API endpoint (path+method) or a schema component.
 * The {@code method} field is only present for API endpoints.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenamedItem {
    /**
     * Type of the renamed item.
     * Valid values: "api", "schema"
     */
    private String type;

    /**
     * Original name before renaming
     */
    private String original;

    /**
     * New name after renaming (e.g., "User" → "User-import")
     */
    private String renamed;

    /**
     * HTTP method for API endpoints (GET, POST, etc.).
     * Only present when type is "api".
     */
    private String method;
}