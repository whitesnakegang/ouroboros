package kr.co.ouroboros.core.rest.tryit.api.dto;

import kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for try summary retrieval API.
 * GET /ouro/tries/{tryId}
 * 
 * Contains only summary metadata without trace spans or analysis issues.
 */
@Data
@Builder
public class TrySummaryResponse {
    
    /**
     * Try session ID.
     */
    private String tryId;
    
    /**
     * Trace ID from Tempo.
     */
    private String traceId;
    
    /**
     * Status of the analysis.
     */
    private AnalysisStatus status;
    
    /**
     * HTTP status code of the response.
     */
    private Integer statusCode;
    
    /**
     * Total duration of the request in milliseconds.
     */
    private Long totalDurationMs;
    
    /**
     * Total number of spans in the trace.
     */
    private Integer spanCount;
    
    /**
     * Total number of detected issues.
     */
    private Integer issueCount;
    
    /**
     * Error message if analysis failed.
     */
    private String error;
}

