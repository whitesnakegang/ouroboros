package kr.co.ouroboros.core.rest.config;

import java.util.LinkedHashMap;
import kr.co.ouroboros.core.global.annotation.ApiState;
import kr.co.ouroboros.core.global.annotation.ApiState.State;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OpenApiCustomizerConfig {

    /**
         * Adds ApiState-derived metadata to an OpenAPI Operation's extensions when a handler method is annotated with {@code ApiState}.
         *
         * <p>If the handler method has an {@code ApiState} annotation, the returned customizer attaches a map under the extension key
         * {@code "ouro-api-state"} containing the keys {@code state} and {@code tag}. {@code state} is the annotation's enum name; for
         * non-COMPLETED states the customizer sets {@code state} to {@code "Mock"} and places the enum name in {@code tag}. If the
         * annotation is absent, the Operation is returned unchanged.
         *
         * @return the {@code OperationCustomizer} that injects the {@code "ouro-api-state"} metadata map into an Operation's extensions
         */

    @Bean
    public OperationCustomizer apiOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiState apiState = handlerMethod.getMethodAnnotation(ApiState.class);

            if (apiState == null) {
                return operation;
            }

            if (operation.getExtensions() == null) {
                operation.setExtensions(new LinkedHashMap<>());
            }

            if (apiState.state() == State.COMPLETED) {
                operation.getExtensions().put("x-ouroboros-progress", "COMPLETED");
                operation.getExtensions().put("x-ouroboros-tag", "");
            } else {
                operation.getExtensions().put("x-ouroboros-progress", "MOCK");
                operation.getExtensions().put("x-ouroboros-tag", apiState.state().name());
            }

            return operation;
        };
    }

}