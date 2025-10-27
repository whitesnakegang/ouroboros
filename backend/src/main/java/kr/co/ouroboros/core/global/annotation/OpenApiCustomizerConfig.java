package kr.co.ouroboros.core.global.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for OpenAPI operation customizers.
 * <p>
 * Provides beans that customize OpenAPI operations by reading {@link ApiState} annotations
 * and adding metadata to the generated OpenAPI specification.
 * This class is no longer a {@code @Configuration} and its beans are registered
 * in {@link kr.co.ouroboros.core.global.config.OuroborosAutoConfiguration}.
 *
 * @since 0.0.1
 */
public class OpenApiCustomizerConfig {

    /**
     * Creates an {@code OperationCustomizer} that reads {@code @ApiState} annotations
     * and injects state metadata into OpenAPI operations.
     * <p>
     * The generated extension includes {@code state} (enum name), {@code owner},
     * and {@code description} fields. If the annotation is not present, the operation
     * is not modified.
     *
     * @return an {@code OperationCustomizer} that adds {@code "ouro-api-state"} metadata
     *         to operations based on {@code @ApiState} annotations
     */
    @Bean
    public OperationCustomizer apiOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiState apiState = handlerMethod.getMethodAnnotation(ApiState.class);

            if(apiState == null){

                return operation;
            }

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("state", apiState.state().name());
            meta.put("owner", apiState.owner());
            meta.put("description", apiState.description());

            if (operation.getExtensions() == null) {
                operation.setExtensions(new LinkedHashMap<>());
            }
            operation.getExtensions().put("ouro-api-state", meta);

            return operation;
        };
    }

}