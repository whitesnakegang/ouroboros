package kr.co.ouroboros.core.global.annotation;

import java.lang.reflect.Method;
import org.springdoc.core.filters.GlobalOpenApiMethodFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Component
public class ApiStateGlobalMethodFilter implements GlobalOpenApiMethodFilter {

    /**
     * 주어진 메서드에 `@ApiState` 애노테이션이 존재하는지 판별한다.
     *
     * @param method 검사할 리플렉션 `Method` 객체
     * @return {@code true}이면 메서드에 `@ApiState` 애노테이션이 존재하고, {@code false}이면 존재하지 않습니다.
     */
    @Override
    public boolean isMethodToInclude(Method method) {
        ApiState apiState = AnnotatedElementUtils.findMergedAnnotation(method, ApiState.class);
        return apiState != null;
    }
}