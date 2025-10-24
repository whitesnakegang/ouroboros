package kr.co.ouroboros.core.global.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiCustomizerConfig {

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
