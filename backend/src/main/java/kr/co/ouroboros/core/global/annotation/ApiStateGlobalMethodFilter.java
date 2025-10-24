package kr.co.ouroboros.core.global.annotation;

import java.lang.reflect.Method;
import org.springdoc.core.filters.GlobalOpenApiMethodFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Component
public class ApiStateGlobalMethodFilter implements GlobalOpenApiMethodFilter {

    @Override
    public boolean isMethodToInclude(Method method) {
        ApiState apiState = AnnotatedElementUtils.findMergedAnnotation(method, ApiState.class);
        return apiState != null;
    }
}
