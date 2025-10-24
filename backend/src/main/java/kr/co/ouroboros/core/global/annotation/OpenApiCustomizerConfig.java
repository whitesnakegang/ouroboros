package kr.co.ouroboros.core.global.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
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

//                String base = Optional.ofNullable(operation.getDescription()).orElse("");
//                String extra = """
//                        **ApiOn**
//                        - state : %s
//                        - owner : `%s`
//                        - description : `%s`
//                        """.formatted(apiState.state(), apiState.owner(), apiState.description());
//                operation.setDescription((base.isBlank() ? "" : base + "\n\n") + extra);
//                if (!apiState.owner().isBlank()) {
//                    operation.addTagsItem("owner:" + apiState.owner());
//                }
//                operation.addTagsItem("state:" + apiState.state().name());
            return operation;
        };
    }

}
