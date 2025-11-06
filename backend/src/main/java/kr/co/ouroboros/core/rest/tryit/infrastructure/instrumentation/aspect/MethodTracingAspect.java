package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect;

import io.micrometer.observation.Observation;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.lang.reflect.Method;

/**
 * AspectJ aspect for method-level tracing.
 * <p>
 * This aspect intercepts all method calls in allowed packages and creates
 * OpenTelemetry spans for each method invocation.
 * <p>
 * Unlike Spring AOP, AspectJ can track self-invocations (methods called
 * within the same class) because it uses bytecode weaving instead of proxies.
 * <p>
 * <b>Note:</b> This aspect is automatically enabled when AspectJ is available
 * on the classpath. Users only need to add the AspectJ Gradle/Maven plugin
 * to enable compile-time weaving.
 */
@Slf4j
@Aspect
@ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
public class MethodTracingAspect {

    private final MethodTracingSupport support;

    public MethodTracingAspect(
            ObjectProvider<io.micrometer.observation.ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties properties
    ) {
        this.support = new MethodTracingSupport(observationRegistryProvider, properties);
        log.info("✅ MethodTracingAspect initialized - AspectJ mode enabled");
        log.info("   Allowed packages: {}", properties.getAllowedPackages());
        log.info("   Method tracing enabled: {}", properties.isEnabled());
        log.info("   AspectJ weaving is required for this aspect to work");
        log.info("   Make sure you have added the AspectJ Gradle/Maven plugin to your build");
        
        // Verify AspectJ is actually available and working
        try {
            Class<?> ajType = org.aspectj.lang.ProceedingJoinPoint.class;
            log.info("   ✅ AspectJ ProceedingJoinPoint class found: {} (classloader: {})", 
                    ajType.getName(), ajType.getClassLoader());
            
            // Check if this aspect class is loaded by AspectJ weaver
            Class<?> thisClass = this.getClass();
            log.info("   ✅ MethodTracingAspect class: {} (classloader: {})", 
                    thisClass.getName(), thisClass.getClassLoader());
            
            // Try to verify AspectJ weaving is active by checking if we can access AspectJ internals
            try {
                Class.forName("org.aspectj.weaver.tools.PointcutExpression");
                log.info("   ✅ AspectJ weaver classes found - weaving should be active");
            } catch (ClassNotFoundException e) {
                log.warn("   ⚠️  AspectJ weaver classes not found - weaving may not be active");
            }
            
            // Check if AspectJ LTW is actually active by checking if classes are being woven
            try {
                // Try to check if OrderService class has been woven
                Class<?> orderServiceClass = Class.forName("com.c102.ourotest.service.OrderService");
                log.info("   ✅ OrderService class found: {} (classloader: {})", 
                        orderServiceClass.getName(), orderServiceClass.getClassLoader());
                
                // Check if class has been woven by AspectJ (this is a heuristic check)
                // Woven classes typically have AspectJ-related annotations or methods
                try {
                    // Try to check if the class has been woven by looking for AspectJ-related methods
                    Method[] methods = orderServiceClass.getDeclaredMethods();
                    log.info("   📋 OrderService methods: {}", java.util.Arrays.stream(methods)
                            .map(Method::getName)
                            .collect(java.util.stream.Collectors.joining(", ")));
                } catch (Exception e) {
                    log.debug("   Could not check OrderService methods: {}", e.getMessage());
                }
            } catch (ClassNotFoundException e) {
                log.debug("   OrderService class not found (expected if not in classpath)");
            }
        } catch (Exception e) {
            log.warn("   ⚠️  Could not verify AspectJ setup: {}", e.getMessage());
        }
    }

    /**
     * Intercepts all method executions in allowed packages.
     * <p>
     * Excludes framework packages to avoid performance issues and conflicts.
     */
    @Around("execution(* *(..)) && " +
            "!within(org.springframework..*) && " +
            "!within(org.apache..*) && " +
            "!within(javax..*) && " +
            "!within(jakarta..*) && " +
            "!within(java..*) && " +
            "!within(sun..*) && " +
            "!within(com.sun..*) && " +
            "!within(io.opentelemetry..*) && " +
            "!within(io.micrometer..*) && " +
            "!within(com.fasterxml..*) && " +
            "!within(ch.qos..*) && " +
            "!within(org.slf4j..*) && " +
            "!within(net.datafaker..*) && " +
            "!within(kr.co.ouroboros..*)")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        String methodName = joinPoint.getSignature().getName();
        String className = targetClass.getName();
        String joinPointKind = joinPoint.getKind();
        
        // CRITICAL: Log ALL intercepted methods in com.c102.ourotest package at INFO level
        // to debug why test3() is not being intercepted
        if (className.startsWith("com.c102.ourotest")) {
            log.info("🔍 AspectJ intercepted method in com.c102.ourotest package: {}.{} (kind: {})", className, methodName, joinPointKind);
            log.info("   Stack trace (first 5 frames):");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                log.info("      [{}] {}", i, stackTrace[i].toString());
            }
        }
        
        // IMPORTANT: Log ALL intercepted methods in allowed packages at INFO level,
        // especially for debugging internal calls (self-invocations)
        boolean isAllowedPackage = support.isAllowedPackage(targetClass);
        
        if (isAllowedPackage) {
            // Log at INFO level for methods in allowed packages to confirm they're being intercepted
            log.info("🔍 AspectJ intercepted method in allowed package: {}.{} (kind: {})", className, methodName, joinPointKind);
            log.debug("   Signature: {}", joinPoint.getSignature().toShortString());
            log.debug("   Target class: {}, Actual class: {}", targetClass.getName(), joinPoint.getTarget().getClass().getName());
            log.debug("   Stack trace depth: {}", Thread.currentThread().getStackTrace().length);
        } else {
            // For debugging: log even non-allowed methods if they're in user packages
            if (className.startsWith("com.c102.ourotest")) {
                log.info("⚠️  AspectJ intercepted method in user package but not allowed: {}.{} (kind: {})", className, methodName, joinPointKind);
            }
            log.debug("🔍 AspectJ intercepted method: {}.{} (signature: {})", className, methodName, joinPoint.getSignature().toShortString());
            log.debug("⏭️  Skipping method {} in class {} (not in allowed packages)", methodName, className);
            return joinPoint.proceed();
        }

        log.info("✅ Tracing method {} in class {} (allowed package)", methodName, className);
        log.debug("   Method modifiers: {}", java.lang.reflect.Modifier.toString(targetClass.getModifiers()));

        Method method;
        try {
            method = getMethod(joinPoint, targetClass);
            log.debug("   Retrieved method: {} (modifiers: {})", method.getName(), java.lang.reflect.Modifier.toString(method.getModifiers()));
        } catch (Exception e) {
            log.warn("⚠️  Failed to get method {} from class {}: {}. Proceeding without tracing.",
                    methodName, targetClass.getName(), e.getMessage());
            log.debug("   Full exception:", e);
            return joinPoint.proceed();
        }

        String effectiveClassName = support.resolveEffectiveClassName(targetClass, className);
        Object[] args = joinPoint.getArgs();

        log.debug("   Creating observation for: {}.{}", effectiveClassName, methodName);
        Observation observation = support.createObservation(effectiveClassName, methodName, method, args);

        try (Observation.Scope ignored = observation.openScope()) {
            return joinPoint.proceed();
        } catch (Throwable t) {
            observation.error(t);
            throw t;
        } finally {
            observation.stop();
        }
    }

    /**
     * Gets the Method object from the join point.
     * <p>
     * Uses MethodSignature.getMethod() which provides the Method object directly
     * from the AspectJ join point. This is more efficient and reliable than
     * using reflection to look up the method.
     * <p>
     * Note: AspectJ can intercept private methods, and the Method object from
     * MethodSignature is already accessible (no need for setAccessible(true)).
     */
    private Method getMethod(ProceedingJoinPoint joinPoint, Class<?> targetClass) {
        try {
            // MethodSignature provides the Method object directly from the join point
            // This is the most reliable way to get the method, especially for private methods
            if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
                org.aspectj.lang.reflect.MethodSignature methodSignature = 
                    (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                return methodSignature.getMethod();
            }
            
            // Fallback: if signature is not MethodSignature, use reflection
            // This should rarely happen with execution() pointcuts
            String methodName = joinPoint.getSignature().getName();
            Class<?>[] parameterTypes = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes();
            
            try {
                return targetClass.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
                // Only setAccessible if needed (for reflection access, not for AspectJ interception)
                method.setAccessible(true);
                return method;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get method: " + joinPoint.getSignature(), e);
        }
    }
}

