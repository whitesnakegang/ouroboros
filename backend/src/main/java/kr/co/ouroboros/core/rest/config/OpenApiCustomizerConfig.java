package kr.co.ouroboros.core.rest.config;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
     * Attach ApiState- and response-derived extensions to an OpenAPI operation when the handler method is annotated with {@code ApiState}.
     *
     * Sets the extension {@code x-ouroboros-progress} to {@code "COMPLETED"} when the annotation's state is {@code State.COMPLETED}, otherwise to {@code "MOCK"}. Sets {@code x-ouroboros-tag} to an empty string for {@code State.COMPLETED}, otherwise to the annotation state's name. If the handler method also has {@code @ApiResponse} or {@code @ApiResponses}, sets {@code x-ouroboros-response} to {@code "use"}. If the handler method has no {@code ApiState} annotation, the operation is returned unchanged.
     *
     * @return the {@code OperationCustomizer} that injects OpenAPI operation extensions based on {@code ApiState} and response annotations
     */

    @Bean
    public OperationCustomizer apiOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiState apiState = handlerMethod.getMethodAnnotation(ApiState.class);
            ApiResponse apiResponse = handlerMethod.getMethodAnnotation(ApiResponse.class);
            ApiResponses apiResponses = handlerMethod.getMethodAnnotation(ApiResponses.class);

            if (apiState == null) {
                return operation;
            }

            if (operation.getExtensions() == null) {
                operation.setExtensions(new LinkedHashMap<>());
            }

            if (apiState.state() == State.COMPLETED) {
                operation.getExtensions().put("x-ouroboros-progress", "completed");
                operation.getExtensions().put("x-ouroboros-tag", "none");
            } else {
                operation.getExtensions().put("x-ouroboros-progress", "mock");
                operation.getExtensions().put("x-ouroboros-tag", apiState.state().name().toLowerCase());
            }

            if(apiResponses != null || apiResponse != null){
                operation.getExtensions().put("x-ouroboros-response", "use");
            }

            return operation;
        };
    }

}