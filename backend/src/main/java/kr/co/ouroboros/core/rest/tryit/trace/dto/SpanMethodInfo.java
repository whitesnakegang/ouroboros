package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing method information parsed from a span.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpanMethodInfo {
    
    /**
     * Class name where this method is located.
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
    @NoArgsConstructor
    @AllArgsConstructor
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

