package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AOP MethodInterceptor for automatic method-level tracing using Micrometer Observation.
 * <p>
 * This interceptor creates OpenTelemetry spans for method invocations in allowed packages,
 * automatically extracting and recording method metadata including:
 * <ul>
 *   <li>Class namespace (code.namespace)</li>
 *   <li>Method name (code.function)</li>
 *   <li>Parameter types and names (code.parameter.*)</li>
 * </ul>
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Resolves effective class name from user-defined interfaces/classes (not proxy classes)</li>
 *   <li>Extracts method parameters with type and name information</li>
 *   <li>Creates observation spans with low cardinality key-value attributes</li>
 *   <li>Handles errors and propagates them while recording in observation</li>
 * </ul>
 * <p>
 * This interceptor is configured by {@link kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingConfig}
 * and only applies to classes in allowed packages as specified in
 * {@link MethodTracingProperties}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class MethodTracingMethodInterceptor implements MethodInterceptor {

    private final MethodTracingSupport support;

    /**
     * Creates a new MethodTracingMethodInterceptor.
     *
     * @param observationRegistryProvider provider for ObservationRegistry
     * @param properties configuration properties
     */
    public MethodTracingMethodInterceptor(
            ObjectProvider<ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties properties
    ) {
        this.support = new MethodTracingSupport(observationRegistryProvider, properties);
    }

    /**
         * Start an Observation for the intercepted method, record method and parameter attributes, execute the invocation, record any thrown error, and stop the Observation.
         *
         * @param invocation the intercepted method invocation
         * @return the result produced by the intercepted method
         * @throws Throwable if the intercepted method throws an exception
         */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> declaringType = invocation.getMethod().getDeclaringClass();
        String declaringTypeName = declaringType.getName();
        
        // Prefer user-defined repository/service/controller interface/class name for className
        Class<?> targetClass = invocation.getThis() != null ? invocation.getThis().getClass() : null;
        String effectiveClassName = support.resolveEffectiveClassName(targetClass, declaringTypeName);
        
        Method method = invocation.getMethod();
        String methodName = method.getName();
        Object[] args = invocation.getArguments();
        
        Observation observation = support.createObservation(effectiveClassName, methodName, method, args);

        try (Observation.Scope ignored = observation.openScope()) {
            return invocation.proceed();
        } catch (Throwable t) {
            observation.error(t);
            throw t;
        } finally {
            observation.stop();
        }
    }
}
