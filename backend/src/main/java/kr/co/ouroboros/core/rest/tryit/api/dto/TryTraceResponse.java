package kr.co.ouroboros.core.rest.tryit.api.dto;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for try trace retrieval API.
 * GET /ouro/tries/{tryId}/trace
 * 
 * Contains only trace information (spans) without analysis issues.
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

