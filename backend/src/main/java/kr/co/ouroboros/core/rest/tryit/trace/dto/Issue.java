package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Detected issue (bottleneck) in the trace.
 */
@Data
@Builder
public class Issue {
    
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

