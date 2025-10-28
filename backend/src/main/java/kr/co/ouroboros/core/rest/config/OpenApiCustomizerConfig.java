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

    @Bean
    public OpenAPI springDocOpenApi(OpenAPIService openApiService) {

        // 1. OpenAPIService가 모든 스캔을 수행하도록 강제
        //    (GroupedOpenApi.DEFAULT_GROUP_NAME은 "default"입니다)
        openApiService.build(Locale.KOREA);

        // 2. OpenAPIService 내부에 캐시된 OpenAPI 객체를 가져와 빈으로 반환
        //    (getOpenApi()는 내부적으로 캐시된 맵에서 값을 가져옵니다)
        return openApiService.getCachedOpenAPI(Locale.KOREA);
    }

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
     * Creates an {@code OperationCustomizer} bean that reads the {@code ApiState} annotation from handler methods and injects state metadata into the OpenAPI {@code Operation} as an extension.
     * <p>
     * The generated extension contains a map with the fields {@code state} (enum name), {@code owner}, and {@code description}. If the annotation is not present, the {@code Operation} remains
     * unchanged.
     *
     * @return an {@code OperationCustomizer} that adds an {@code "ouro-api-state"} metadata map to the {@code Operation}'s extensions based on the method's {@code ApiState}
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