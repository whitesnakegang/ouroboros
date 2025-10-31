package kr.co.ouroboros.core.rest.tryit.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for try result retrieval API.
 * GET /ouro/tries/{tryId}
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
     * Total duration of the request in milliseconds.
     */
    private Long totalDurationMs;
    
    /**
     * HTTP status code of the response.
     */
    private Integer statusCode;
    
    /**
     * Hierarchical span tree showing all method calls.
     */
    private List<SpanNode> spans;
    
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
     * Span node in the hierarchical tree.
     */
    @Data
    @Builder
    public static class SpanNode {
        
        /**
         * Span name (e.g., "OrderController.getOrder").
         */
        private String name;
        
        /**
         * Class name where this span/method is located.
         */
        private String className;
        
        /**
         * Method name.
         */
        private String methodName;
        
        /**
         * Method parameters (if available).
         */
        private List<Parameter> parameters;
        
        /**
         * Parameter information.
         */
        @Data
        @Builder
        public static class Parameter {
            /**
             * Parameter type.
             */
            private String type;
            
            /**
             * Parameter name.
             */
            private String name;
        }
        
        /**
         * Total duration in milliseconds (including children).
         */
        private Long durationMs;
        
        /**
         * Self duration in milliseconds (excluding children execution time).
         */
        private Long selfDurationMs;
        
        /**
         * Percentage of total trace duration (including children).
         */
        private Double percentage;
        
        /**
         * Self percentage of total trace duration (excluding children).
         */
        private Double selfPercentage;
        
        /**
         * Child spans (nested method calls).
         */
        private List<SpanNode> children;
        
        /**
         * Span kind (e.g., SERVER, CLIENT, INTERNAL).
         */
        private String kind;
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
         * Issue summary description.
         */
        private String summary;
        
        /**
         * Affected span name.
         */
        private String spanName;
        
        /**
         * Duration in milliseconds (if applicable).
         */
        private Long durationMs;
        
        /**
         * Evidence supporting the issue detection.
         */
        private List<String> evidence;
        
        /**
         * Recommendation for fixing the issue.
         */
        private String recommendation;
        
        /**
         * Issue type.
         */
        public enum Type {
            SLOW_HTTP,
            SLOW_DATABASE,
            N_PLUS_ONE,
            SLOW_SPAN,
            DB_QUERY_SLOW,
            HIGH_LATENCY
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

