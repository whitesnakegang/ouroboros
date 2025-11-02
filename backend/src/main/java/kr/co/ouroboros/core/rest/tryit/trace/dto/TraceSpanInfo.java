package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Internal DTO representing span information extracted from TraceDTO.
 * Used as an intermediate representation for trace processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSpanInfo {
    
    /**
     * Span ID.
     */
    private String spanId;
    
    /**
     * Parent span ID.
     */
    private String parentSpanId;
    
    /**
     * Span name.
     */
    private String name;
    
    /**
     * Span kind (e.g., SERVER, CLIENT, INTERNAL).
     */
    private String kind;
    
    /**
     * Start time in nanoseconds.
     */
    private Long startTimeNanos;
    
    /**
     * End time in nanoseconds.
     */
    private Long endTimeNanos;
    
    /**
     * Duration in nanoseconds.
     */
    private Long durationNanos;
    
    /**
     * Duration in milliseconds.
     */
    private Long durationMs;
    
    /**
     * Span attributes as key-value pairs.
     */
    private Map<String, String> attributes;
}

