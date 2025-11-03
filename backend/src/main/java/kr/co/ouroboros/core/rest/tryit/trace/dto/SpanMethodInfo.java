package kr.co.ouroboros.core.rest.tryit.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing method information parsed from a span.
 * <p>
 * Contains parsed method information extracted from span names,
 * including class name, method name, and parameters.
 *
 * @author Ouroboros Team
 * @since 0.0.1
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
     * Method parameter information.
     * <p>
     * Contains parameter type and name extracted from method signature.
     *
     * @author Ouroboros Team
     * @since 0.0.1
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

