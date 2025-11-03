package kr.co.ouroboros.ui.rest.tryit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for Try method list API.
 * <p>
 * Response for GET /ouro/tries/{tryId}/methods
 * <p>
 * Contains paginated list of methods sorted by selfDurationMs (descending),
 * optimized for method-level performance analysis.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Data
@Builder
public class TryMethodListResponse {
    
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
     * Total number of methods.
     */
    private Integer totalCount;
    
    /**
     * Current page number (0-based).
     */
    private Integer page;
    
    /**
     * Page size.
     */
    private Integer size;
    
    /**
     * Whether there are more pages.
     */
    private Boolean hasMore;
    
    /**
     * Flat list of methods sorted by selfDurationMs (descending).
     */
    private List<MethodInfo> methods;
    
    /**
     * Individual method information.
     * <p>
     * Contains method execution details including name, class, parameters,
     * duration, and percentage of total trace time.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    @Builder
    public static class MethodInfo {

        /**
         * Span ID (unique identifier for the span).
         * Used to match spans between /methods and /trace API responses.
         */
        private String spanId;
        
        /**
         * Span name (e.g., "OrderController.getOrder").
         */
        private String name;
        
        /**
         * Method name.
         */
        private String methodName;
        
        /**
         * Class name where this method is located.
         */
        private String className;
        
        /**
         * Method parameters (if available).
         */
        private List<Parameter> parameters;
        
        /**
         * Self duration in milliseconds (excluding children execution time).
         */
        private Long selfDurationMs;
        
        /**
         * Self percentage of total trace duration.
         */
        private Double selfPercentage;
        
        /**
         * Method parameter information.
         * <p>
         * Contains parameter type and name extracted from method signature.
         *
         * @author Ouroboros Team
         * @since 0.0.1
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
    }
}

