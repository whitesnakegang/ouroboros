package kr.co.ouroboros.core.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark the development state of REST API endpoints.
 * <p>
 * Use this annotation on controller methods to indicate the current implementation status.
 * This metadata is included in the generated OpenAPI documentation via
 * {@link kr.co.ouroboros.core.rest.config.OpenApiCustomizerConfig#apiOperationCustomizer()}.
 *
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiState {

    /**
     * The current development state of the API endpoint.
     *
     * @return the state enum value
     */
    State state();

    /**
     * Enumeration of possible API development states.
     */
    enum State {
        /** API is currently being implemented */
        IMPLEMENTING,
        /** API is under bug fixing */
        BUG_FIXING,
        /** API implementation is completed */
        COMPLETED
    }
}
