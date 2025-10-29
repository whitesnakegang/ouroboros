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
     * Attach ApiState-derived extensions to an OpenAPI operation when the handler method is annotated with {@code ApiState}.
     *
     * Adds or initializes operation extensions and sets:
     * - {@code x-ouroboros-progress} to {@code "COMPLETED"} when the annotation's state is {@code State.COMPLETED}, otherwise {@code "MOCK"}.
     * - {@code x-ouroboros-tag} to an empty string for {@code State.COMPLETED}, otherwise to the annotation state's name.
     *
     * The operation is returned unchanged if the handler method has no {@code ApiState} annotation.
     *
     * @return an {@code OperationCustomizer} that injects the {@code x-ouroboros-progress} and {@code x-ouroboros-tag} extensions based on {@code ApiState}
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
