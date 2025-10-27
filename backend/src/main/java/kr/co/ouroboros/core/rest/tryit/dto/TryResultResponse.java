package kr.co.ouroboros.core.rest.tryit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for try result retrieval API.
 * GET /ouroboros/tries/{tryId}
 */
@Data
@Builder
public class TryResultResponse {
    
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
    private Status status;
    
    /**
     * Timestamp when the try was created.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when the analysis was completed.
     */
    private Instant analyzedAt;
    
    /**
     * Duration of the request in milliseconds.
     */
    private Long durationMs;
    
    /**
     * Detected issues (bottlenecks).
     */
    private List<Issue> issues;
    
    /**
     * Span count in the trace.
     */
    private Integer spanCount;
    
    /**
     * Error message if analysis failed.
     */
    private String error;
    
    /**
     * Analysis status.
     */
    public enum Status {
        PENDING,
        COMPLETED,
        FAILED,
        NOT_FOUND
    }
    
    /**
     * Detected issue (bottleneck).
     */
    @Data
    @Builder
    public static class Issue {
        
        /**
         * Issue type.
         */
        private Type type;
        
        /**
         * Issue severity.
         */
        private Severity severity;
        
        /**
         * Issue description.
         */
        private String description;
        
        /**
         * Affected span name.
         */
        private String spanName;
        
        /**
         * Duration in milliseconds (if applicable).
         */
        private Long durationMs;
        
        /**
         * Issue type.
         */
        public enum Type {
            SLOW_HTTP,
            SLOW_DATABASE,
            N_PLUS_ONE,
            SLOW_SPAN
        }
        
        /**
         * Issue severity.
         */
        public enum Severity {
            LOW,
            MEDIUM,
            HIGH,
            CRITICAL
        }
    }
}
