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
     * Produces an OperationCustomizer that adds OpenAPI operation extensions based on a handler method's {@code ApiState} and response annotations.
     *
     * When the handler method is annotated with {@code @ApiState}:
     * - sets extension {@code x-ouroboros-progress} to {@code "completed"} if the state is {@code State.COMPLETED}, otherwise to {@code "mock"}.
     * - sets extension {@code x-ouroboros-tag} to {@code "none"} if the state is {@code State.COMPLETED}, otherwise to the state's name in lowercase.
     * - if the handler method has {@code @ApiResponse} or {@code @ApiResponses}, sets extension {@code x-ouroboros-response} to {@code "use"}.
     * If the handler method has no {@code @ApiState}, the operation is returned unchanged.
     *
     * @return the {@code OperationCustomizer} that injects the described OpenAPI operation extensions
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