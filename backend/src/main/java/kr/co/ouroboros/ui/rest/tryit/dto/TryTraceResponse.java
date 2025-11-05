package kr.co.ouroboros.ui.rest.tryit.dto;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for Try trace retrieval API.
 * <p>
 * Response for GET /ouro/tries/{tryId}/trace
 * <p>
 * Contains hierarchical trace information (spans) without analysis issues.
 * Optimized for call trace visualization (toggle tree view).
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Data
@Builder
public class TryTraceResponse {
    
    /**
     * Try session ID.
     */
    private String tryId;
    
    /**
     * Trace ID from Tempo.
     */
    private String traceId;
    
    /**
     * Total duration of the request in milliseconds.
     */
    private Long totalDurationMs;
    
    /**
     * Hierarchical span tree showing all method calls.
     */
    private List<SpanNode> spans;
}

