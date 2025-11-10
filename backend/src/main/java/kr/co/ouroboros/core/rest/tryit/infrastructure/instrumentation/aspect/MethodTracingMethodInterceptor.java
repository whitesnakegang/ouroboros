package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.rest.tryit.config.properties.MethodTracingProperties;
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
 * This interceptor is configured by {@link kr.co.ouroboros.core.rest.tryit.config.MethodTracingConfig}
 * and only applies to classes in allowed packages as specified in
 * {@link MethodTracingProperties}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class MethodTracingMethodInterceptor implements MethodInterceptor {

    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;
    @SuppressWarnings("unused")
    private final MethodTracingProperties properties; // reserved for future toggles

    /**
     * Gets or creates ObservationRegistry for creating observations.
     * <p>
     * Attempts to get ObservationRegistry from Spring context;
     * creates a new one if not available.
     *
     * @return ObservationRegistry instance
     */
    private ObservationRegistry registry() {
        ObservationRegistry reg = observationRegistryProvider.getIfAvailable();
        return (reg != null) ? reg : ObservationRegistry.create();
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
        String effectiveClassName = resolveEffectiveClassName(invocation, declaringTypeName);
        String methodName = invocation.getMethod().getName();
        String spanName = simpleName(effectiveClassName) + "." + methodName;

        Observation observation = Observation.start(spanName, registry())
                .lowCardinalityKeyValue("code.namespace", effectiveClassName)
                .lowCardinalityKeyValue("code.function", methodName);

        // 파라미터 메타데이터(타입/이름)
        try {
            Method method = invocation.getMethod();
            Parameter[] params = method.getParameters();
            Object[] args = invocation.getArguments();
            observation.lowCardinalityKeyValue("code.parameters.count", String.valueOf(params.length));
            for (int i = 0; i < params.length; i++) {
                Class<?> pt = params[i].getType();
                String typeName = pt.getSimpleName();
                // If erased to Object (e.g., CrudRepository<ID>), prefer runtime argument type
                if ((pt == Object.class || "Object".equals(typeName)) && args != null && i < args.length && args[i] != null) {
                    typeName = args[i].getClass().getSimpleName();
                }
                String paramName = params[i].getName();
                observation.lowCardinalityKeyValue("code.parameter." + i + ".type", typeName);
                observation.lowCardinalityKeyValue("code.parameter." + i + ".name", paramName);
            }
        } catch (Throwable t) {
            log.debug("Failed to enrich parameters for {}: {}", spanName, t.getMessage());
        }

        try (Observation.Scope ignored = observation.openScope()) {
            return invocation.proceed();
        } catch (Throwable t) {
            observation.error(t);
            throw t;
        } finally {
            observation.stop();
        }
    }

    /**
     * Determine the effective class name to attribute method traces to, preferring user-defined
     * interfaces or superclasses from configured package prefixes over proxy class names.
     *
     * @param invocation    the method invocation containing the target instance (may be a proxy)
     * @param fallbackFqcn  the fully qualified class name to return if no user-defined class is found
     * @return the fully qualified name of a user-defined interface or superclass from allowed packages,
     *         or {@code fallbackFqcn} if none is found
     */
    private String resolveEffectiveClassName(MethodInvocation invocation, String fallbackFqcn) {
        // Prefer user-defined interface/class under allowed packages if present on proxy
        Class<?> targetClass = invocation.getThis() != null ? invocation.getThis().getClass() : null;
        if (targetClass != null) {
            // Check interfaces first (JDK dynamic proxies for repositories)
            for (Class<?> itf : targetClass.getInterfaces()) {
                String n = itf.getName();
                if (isUserPackage(n)) return n;
            }
            // Check superclass chain (CGLIB proxies)
            Class<?> sc = targetClass.getSuperclass();
            while (sc != null && sc != Object.class) {
                String n = sc.getName();
                if (isUserPackage(n)) return n;
                sc = sc.getSuperclass();
            }
        }
        return fallbackFqcn;
    }

    /**
     * Checks if the fully qualified class name belongs to allowed packages.
     * <p>
     * Determines whether a class name starts with any of the allowed package
     * prefixes specified in {@link MethodTracingProperties#getAllowedPackages()}.
     *
     * @param fqcn Fully qualified class name to check
     * @return true if class belongs to allowed packages, false otherwise
     */
    private boolean isUserPackage(String fqcn) {
        // Allowed packages from properties
        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) return false;
        for (String root : properties.getAllowedPackages()) {
            if (fqcn.startsWith(root)) return true;
        }
        return false;
    }

    /**
     * Extracts the simple class name from a fully qualified class name.
     *
     * @param fqcn fully qualified class name, may be {@code null}
     * @return the substring after the last '.' character; if no '.' is present returns the original input;
     *         if {@code fqcn} is {@code null} returns an empty string
     */
    private String simpleName(String fqcn) {
        int idx = fqcn != null ? fqcn.lastIndexOf('.') : -1;
        return idx >= 0 ? fqcn.substring(idx + 1) : (fqcn != null ? fqcn : "");
    }
}
