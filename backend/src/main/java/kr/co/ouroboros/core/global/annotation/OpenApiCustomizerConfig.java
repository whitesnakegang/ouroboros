package kr.co.ouroboros.core.global.annotation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiCustomizerConfig {

    /**
     * 핸들러 메서드에서 {@code ApiState} 애노테이션을 읽어 OpenAPI Operation에 상태 메타정보를 확장으로 주입하는 {@code OperationCustomizer} 빈을 생성한다.
     *
     * 생성되는 확장에는 {@code state} (열거형 이름), {@code owner}, {@code description} 필드를 가진 맵이 포함되며,
     * 해당 애노테이션이 없으면 Operation을 변경하지 않는다.
     *
     * @return 핸들러 메서드의 {@code ApiState}를 기반으로 Operation의 extensions에 {@code "ouro-api-state"} 메타맵을 추가하는 {@code OperationCustomizer}
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