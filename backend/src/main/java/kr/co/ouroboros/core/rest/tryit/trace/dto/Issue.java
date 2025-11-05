package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Detected issue (bottleneck) in the trace.
 * <p>
 * Represents a performance issue detected during trace analysis,
 * including type, severity, evidence, and recommendations.
 *
 * @author Ouroboros Team
 * @since 0.0.1
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
     * <p>
     * Categorizes the type of performance issue detected.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    public enum Type {
        /**
         * Slow HTTP call detected.
         */
        SLOW_HTTP,
        
        /**
         * Slow database operation detected.
         */
        SLOW_DATABASE,
        
        /**
         * N+1 query problem detected.
         */
        N_PLUS_ONE,
        
        /**
         * Slow span execution detected.
         */
        SLOW_SPAN,
        
        /**
         * Slow database query detected.
         */
        DB_QUERY_SLOW,
        
        /**
         * High latency detected.
         */
        HIGH_LATENCY
    }
    
    /**
     * Issue severity level.
     * <p>
     * Indicates the severity of the detected performance issue.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    public enum Severity {
        /**
         * Low severity issue.
         */
        LOW,
        
        /**
         * Medium severity issue.
         */
        MEDIUM,
        
        /**
         * High severity issue.
         */
        HIGH,
        
        /**
         * Critical severity issue requiring immediate attention.
         */
        CRITICAL
    }
}

