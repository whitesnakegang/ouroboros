package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Span node in the hierarchical trace tree.
 * Represents a method call with its metadata and nested calls.
 */
@Data
@Builder
public class SpanNode {
    
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

