package kr.co.ouroboros.core.global.tryit.infrastructure.instrumentation.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.global.tryit.config.properties.MethodTracingProperties;
import kr.co.ouroboros.core.global.tryit.infrastructure.instrumentation.context.TryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * AspectJ aspect for automatic method-level tracing using Micrometer Observation.
 * <p>
 * This aspect creates OpenTelemetry spans for method invocations in allowed packages,
 * automatically extracting and recording method metadata using AspectJ compile-time weaving.
 * <p>
 * <b>Differences from Spring AOP Mode:</b>
 * <ul>
 *   <li>Traces self-invocation (same class method calls)</li>
 *   <li>Traces private methods</li>
 *   <li>Traces static methods</li>
 *   <li>No proxy overhead at runtime</li>
 *   <li>Requires AspectJ compile-time weaving setup in user project</li>
 * </ul>
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Resolves effective class name from declaring type</li>
 *   <li>Extracts method parameters with type and name information</li>
 *   <li>Creates observation spans with low cardinality key-value attributes</li>
 *   <li>Handles errors and propagates them while recording in observation</li>
 *   <li>Only traces when TryContext is active (Try request in progress)</li>
 * </ul>
 * <p>
 * <b>Important:</b> This aspect uses AspectJ's singleton pattern via {@code aspectOf()}.
 * It is NOT managed as a Spring bean. Dependencies are injected via
 * {@link kr.co.ouroboros.core.global.tryit.config.AspectJMethodTracingConfig}.
 * <p>
 * This aspect is configured by {@link kr.co.ouroboros.core.global.tryit.config.AspectJMethodTracingConfig}
 * and only applies to classes in allowed packages as specified in
 * {@link MethodTracingProperties}.
 *
 * @author Ouroboros Team
 * @since 1.1.0
 */
@Slf4j
@Aspect
public class MethodTracingAspect {

    private ObjectProvider<ObservationRegistry> observationRegistryProvider;
    private MethodTracingProperties properties;

    /**
     * Sets the ObservationRegistry provider for creating observations.
     * <p>
     * Called by {@link kr.co.ouroboros.core.global.tryit.config.AspectJMethodTracingConfig}
     * during Spring context initialization.
     *
     * @param observationRegistryProvider provider for ObservationRegistry
     */
    public void setObservationRegistryProvider(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        this.observationRegistryProvider = observationRegistryProvider;
    }

    /**
     * Sets the method tracing configuration properties.
     * <p>
     * Called by {@link kr.co.ouroboros.core.global.tryit.config.AspectJMethodTracingConfig}
     * during Spring context initialization.
     *
     * @param properties method tracing configuration properties
     */
    public void setProperties(MethodTracingProperties properties) {
        this.properties = properties;
    }

    /**
     * Pointcut for all method executions excluding Ouroboros SDK classes.
     * <p>
     * This pointcut matches:
     * <ul>
     *   <li>All methods (public, private, protected, package-private)</li>
     *   <li>Static methods</li>
     *   <li>Methods in any package except kr.co.ouroboros.*</li>
     *   <li>Excludes this aspect class itself to prevent circular dependency</li>
     * </ul>
     * <p>
     * Additional filtering by allowed packages is done in the advice method.
     */
    @Pointcut("execution(* *(..)) && !within(kr.co.ouroboros..*) && !within(MethodTracingAspect)")
    public void allNonSdkMethods() {}

    /**
     * Around advice for method tracing with Micrometer Observation.
     * <p>
     * Creates observation spans only when:
     * <ul>
     *   <li>TryContext has a tryId (Try request is active)</li>
     *   <li>Method tracing is enabled in properties</li>
     *   <li>Method's class is in allowed packages</li>
     * </ul>
     * <p>
     * The observation includes:
     * <ul>
     *   <li>Span name: SimpleClassName.methodName</li>
     *   <li>code.namespace: Fully qualified class name</li>
     *   <li>code.function: Method name</li>
     *   <li>code.parameter.*.type: Parameter types</li>
     *   <li>code.parameter.*.name: Parameter names</li>
     * </ul>
     *
     * @param pjp the proceeding join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("allNonSdkMethods()")
    public Object traceMethod(ProceedingJoinPoint pjp) throws Throwable {
        // Guard 0: Check if dependencies are injected
        if (observationRegistryProvider == null || properties == null) {
            log.debug("Dependencies not injected - observationRegistryProvider: {}, properties: {}",
                     observationRegistryProvider, properties);
            return pjp.proceed();
        }

        // Guard 1: Only trace when Try context is active
        if (!TryContext.hasTryId()) {
            return pjp.proceed();
        }

        // Guard 2: Check if method tracing is enabled
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        // Guard 3: Check if class is in allowed packages
        String className = pjp.getSignature().getDeclaringTypeName();
        if (!isInAllowedPackages(className)) {
            return pjp.proceed();
        }

        // Extract method metadata
        String methodName = pjp.getSignature().getName();
        String spanName = simpleName(className) + "." + methodName;

        // Create observation
        Observation observation = Observation.start(spanName, registry())
                .lowCardinalityKeyValue("code.namespace", className)
                .lowCardinalityKeyValue("code.function", methodName);

        // Enrich with parameter metadata
        enrichParameterMetadata(observation, pjp);

        try (Observation.Scope ignored = observation.openScope()) {
            return pjp.proceed();
        } catch (Throwable t) {
            observation.error(t);
            throw t;
        } finally {
            observation.stop();
        }
    }

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
     * Checks if the fully qualified class name belongs to allowed packages.
     * <p>
     * Determines whether a class name starts with any of the allowed package
     * prefixes specified in {@link MethodTracingProperties#getAllowedPackages()}.
     *
     * @param className fully qualified class name to check
     * @return true if class belongs to allowed packages, false otherwise
     */
    private boolean isInAllowedPackages(String className) {
        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) {
            return false;
        }

        for (String allowedPkg : properties.getAllowedPackages()) {
            if (className.startsWith(allowedPkg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enriches observation with parameter metadata.
     * <p>
     * Attempts to extract and record:
     * <ul>
     *   <li>Parameter count</li>
     *   <li>Parameter types (resolving runtime type for erased generics)</li>
     *   <li>Parameter names</li>
     * </ul>
     * <p>
     * Failures in parameter enrichment are logged but do not prevent tracing.
     *
     * @param observation the observation to enrich
     * @param pjp the proceeding join point
     */
    private void enrichParameterMetadata(Observation observation, ProceedingJoinPoint pjp) {
        try {
            if (pjp.getSignature() instanceof MethodSignature) {
                MethodSignature signature = (MethodSignature) pjp.getSignature();
                Method method = signature.getMethod();
                Parameter[] params = method.getParameters();
                Object[] args = pjp.getArgs();

                observation.lowCardinalityKeyValue("code.parameters.count",
                                                  String.valueOf(params.length));

                for (int i = 0; i < params.length; i++) {
                    Class<?> pt = params[i].getType();
                    String typeName = pt.getSimpleName();

                    // Handle erased generics: if type is Object, prefer runtime argument type
                    if ((pt == Object.class || "Object".equals(typeName)) &&
                        args != null && i < args.length && args[i] != null) {
                        typeName = args[i].getClass().getSimpleName();
                    }

                    String paramName = params[i].getName();
                    observation.lowCardinalityKeyValue("code.parameter." + i + ".type", typeName);
                    observation.lowCardinalityKeyValue("code.parameter." + i + ".name", paramName);
                }
            }
        } catch (Throwable t) {
            String spanName = pjp.getSignature().getDeclaringTypeName() + "." +
                             pjp.getSignature().getName();
            log.debug("Failed to enrich parameters for {}: {}", spanName, t.getMessage());
        }
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
