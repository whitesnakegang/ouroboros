package kr.co.ouroboros.core.rest.filter;

import java.lang.reflect.Method;
import kr.co.ouroboros.core.global.annotation.ApiState;
import org.springdoc.core.filters.GlobalOpenApiMethodFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Global OpenAPI method filter that includes only methods annotated with {@link ApiState}.
 * <p>
 * This filter is used by SpringDoc to determine which API endpoints should be included
 * in the generated OpenAPI documentation based on the presence of the {@code @ApiState} annotation.
 * Registered as a bean in {@link kr.co.ouroboros.core.global.config.OuroborosAutoConfiguration}.
 *
 * @since 0.0.1
 */
public class ApiStateGlobalMethodFilter implements GlobalOpenApiMethodFilter {

    /**
     * Determines whether the given method should be included in OpenAPI documentation.
     * <p>
     * A method is included if it has the {@code @ApiState} annotation.
     *
     * @param method the reflection {@code Method} object to check
     * @return {@code true} if the method has {@code @ApiState} annotation, {@code false} otherwise
     */
    @Override
    public boolean isMethodToInclude(Method method) {
        ApiState apiState = AnnotatedElementUtils.findMergedAnnotation(method, ApiState.class);
        return apiState != null;
    }
}