package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Common support class for method tracing logic shared between Spring AOP and AspectJ.
 * <p>
 * This class contains the core tracing logic that is used by both
 * {@link MethodTracingMethodInterceptor} (Spring AOP) and {@link MethodTracingAspect} (AspectJ).
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class MethodTracingSupport {

    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;
    private final MethodTracingProperties properties;

    /**
     * Gets or creates ObservationRegistry for creating observations.
     *
     * @return ObservationRegistry instance
     */
    public ObservationRegistry getRegistry() {
        ObservationRegistry reg = observationRegistryProvider.getIfAvailable();
        return (reg != null) ? reg : ObservationRegistry.create();
    }

    /**
     * Creates an Observation span for a method invocation.
     *
     * @param effectiveClassName the effective class name
     * @param methodName the method name
     * @param method the method object
     * @param args the method arguments
     * @return the created Observation
     */
    public Observation createObservation(String effectiveClassName, String methodName, Method method, Object[] args) {
        String spanName = simpleName(effectiveClassName) + "." + methodName;

        Observation observation = Observation.start(spanName, getRegistry())
                .lowCardinalityKeyValue("code.namespace", effectiveClassName)
                .lowCardinalityKeyValue("code.function", methodName);

        // 파라미터 메타데이터(타입/이름)
        try {
            Parameter[] params = method.getParameters();
            observation.lowCardinalityKeyValue("code.parameters.count", String.valueOf(params.length));
            for (int i = 0; i < params.length; i++) {
                Class<?> pt = params[i].getType();
                String typeName = pt.getSimpleName();
                // If erased to Object, prefer runtime argument type
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

        return observation;
    }

    /**
     * Checks if a class belongs to allowed packages.
     * <p>
     * Uses the same filtering logic as {@link kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingConfig}
     * to ensure consistency between Spring AOP and AspectJ modes.
     *
     * @param clazz the class to check
     * @return true if class belongs to allowed packages, false otherwise
     */
    public boolean isAllowedPackage(Class<?> clazz) {
        String className = clazz.getName();
        
        if (!properties.isEnabled()) {
            log.debug("⏭️  Package check for {}: tracing disabled", className);
            return false;
        }
        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) {
            log.debug("⏭️  Package check for {}: no allowed packages configured", className);
            return false;
        }

        // SDK 자체는 제외
        if (className.startsWith("kr.co.ouroboros.")) {
            log.debug("⏭️  Package check for {}: SDK class excluded", className);
            return false;
        }

        // 1) 대상 클래스 자체가 허용 패키지인지
        for (String root : properties.getAllowedPackages()) {
            if (className.equals(root) || className.startsWith(root + ".")) {
                log.debug("✅ Package check for {}: matches allowed package {}", className, root);
                return true;
            }
        }

        // 2) 구현 인터페이스 중 허용 패키지에 속하는 것이 있는지
        Class<?>[] ifaces = clazz.getInterfaces();
        for (Class<?> iface : ifaces) {
            String in = iface.getName();
            for (String root : properties.getAllowedPackages()) {
                if (in.equals(root) || in.startsWith(root + ".")) {
                    log.debug("✅ Package check for {}: matches allowed package {} via interface {}", className, root, in);
                    return true;
                }
            }
        }

        // 3) 슈퍼클래스 체인에도 사용자 클래스가 있는지 확인
        Class<?> sc = clazz.getSuperclass();
        while (sc != null && sc != Object.class) {
            String sn = sc.getName();
            if (sn.startsWith("kr.co.ouroboros.")) {
                log.debug("⏭️  Package check for {}: SDK superclass {} excluded", className, sn);
                return false; // SDK 제외 우선
            }
            for (String root : properties.getAllowedPackages()) {
                if (sn.equals(root) || sn.startsWith(root + ".")) {
                    log.debug("✅ Package check for {}: matches allowed package {} via superclass {}", className, root, sn);
                    return true;
                }
            }
            sc = sc.getSuperclass();
        }

        log.debug("⏭️  Package check for {}: no match found in allowed packages {}", className, properties.getAllowedPackages());
        return false;
    }

    /**
     * Resolves the effective class name from a target class.
     *
     * @param targetClass the target class
     * @param fallbackFqcn fallback fully qualified class name
     * @return the effective class name
     */
    public String resolveEffectiveClassName(Class<?> targetClass, String fallbackFqcn) {
        if (targetClass != null) {
            // Check interfaces first
            for (Class<?> itf : targetClass.getInterfaces()) {
                String n = itf.getName();
                if (isUserPackage(n)) return n;
            }
            // Check superclass chain
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
     *
     * @param fqcn Fully qualified class name to check
     * @return true if class belongs to allowed packages, false otherwise
     */
    private boolean isUserPackage(String fqcn) {
        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) {
            return false;
        }
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
    public String simpleName(String fqcn) {
        int idx = fqcn != null ? fqcn.lastIndexOf('.') : -1;
        return idx >= 0 ? fqcn.substring(idx + 1) : (fqcn != null ? fqcn : "");
    }
}

