package kr.co.ouroboros.core.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import kr.co.ouroboros.core.global.annotation.ApiState;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OpenApiCustomizerConfig {

    /**
     * Provide the application's cached OpenAPI model for the Korea locale.
     *
     * Forces the provided OpenAPIService to perform a full scan for Locale.KOREA, then returns the cached OpenAPI instance for that locale.
     *
     * @param openApiService service used to build and retrieve the cached OpenAPI model
     * @return the cached OpenAPI instance for Locale.KOREA
     */
    @Bean
    public OpenAPI springDocOpenApi(OpenAPIService openApiService) {

        // 1. OpenAPIService가 모든 스캔을 수행하도록 강제
        //    (GroupedOpenApi.DEFAULT_GROUP_NAME은 "default"입니다)
        openApiService.build(Locale.KOREA);

        // 2. OpenAPIService 내부에 캐시된 OpenAPI 객체를 가져와 빈으로 반환
        //    (getOpenApi()는 내부적으로 캐시된 맵에서 값을 가져옵니다)
        return openApiService.getCachedOpenAPI(Locale.KOREA);
    }

    /**
     * Creates an OpenApiCustomizer that logs when it is created and iterates all OpenAPI paths.
     *
     * The customizer does not modify the OpenAPI document; it only visits each path entry.
     *
     * @return an OpenApiCustomizer that logs discovery and iterates every path in the OpenAPI model
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            log.info("OpenApiCustomizer found");
            openApi.getPaths()
                    .forEach((path, pathItem) -> {

                    });
        };
    }

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

            Map<String, Object> meta = new LinkedHashMap<>();

            if (apiState.state() == ApiState.State.COMPLETED) {
                meta.put("state", apiState.state()
                        .name());
                meta.put("tag", "");
            } else {
                meta.put("state", "Mock");
                meta.put("tag", apiState.state()
                        .name());
            }

            meta.put("state", apiState.state()
                    .name());

            if (operation.getExtensions() == null) {
                operation.setExtensions(new LinkedHashMap<>());
            }
            operation.getExtensions()
                    .put("ouro-api-state", meta);

            return operation;
        };
    }

}