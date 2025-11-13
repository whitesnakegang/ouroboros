package kr.co.ouroboros.ui.websocket.spec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.ouroboros.core.websocket.common.dto.Operation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AsyncAPI operation operations.
 * <p>
 * Wraps the Operation common DTO with operation name for identification.
 * Progress and diff status are now stored directly in the Operation DTO.
 *
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationResponse {

    /**
     * Operation name (identifier in operations section)
     */
    private String operationName;

    /**
     * Operation definition (from common DTO)
     * <p>
     * Includes x-ouroboros-diff and x-ouroboros-progress fields.
     */
    private Operation operation;

    /**
     * Operation tag indicating the operation type.
     * <p>
     * Possible values:
     * <ul>
     *   <li>"receive": receive-only operation (no reply)</li>
     *   <li>"duplicate": receive with reply operation</li>
     *   <li>"sendto": send-only operation</li>
     * </ul>
     */
    private String tag;
}


