package kr.co.ouroboros.ui.websocket.spec.dto;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for YAML import operation.
 * <p>
 * Provides detailed information about the import results including
 * the number of imported items, renamed items, and a summary message.
 * Used as the data field in the standard {@link GlobalApiResponse}.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportYamlResponse {
    /**
     * Total number of channels successfully imported
     */
    private int importedChannels;

    /**
     * Total number of operations successfully imported
     */
    private int importedOperations;

    /**
     * Number of items (channels, operations, schemas, or messages) that were renamed due to duplicates
     */
    private int renamed;

    /**
     * Human-readable summary message
     * Example: "Successfully imported 5 channels, 3 operations, renamed 2 due to duplicates"
     */
    private String summary;

    /**
     * Detailed list of all renamed items with original and new names
     */
    private List<RenamedItem> renamedList;
}