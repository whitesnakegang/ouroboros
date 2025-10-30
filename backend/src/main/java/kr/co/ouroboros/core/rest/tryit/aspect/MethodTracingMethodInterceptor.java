package kr.co.ouroboros.core.rest.tryit.aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.rest.tryit.config.MethodTracingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@RequiredArgsConstructor
public class MethodTracingMethodInterceptor implements MethodInterceptor {

    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;
    @SuppressWarnings("unused")
    private final MethodTracingProperties properties; // reserved for future toggles

    private ObservationRegistry registry() {
        ObservationRegistry reg = observationRegistryProvider.getIfAvailable();
        return (reg != null) ? reg : ObservationRegistry.create();
    }

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

    private boolean isUserPackage(String fqcn) {
        // Allowed packages from properties
        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) return false;
        for (String root : properties.getAllowedPackages()) {
            if (fqcn.startsWith(root)) return true;
        }
        return false;
    }

    private String simpleName(String fqcn) {
        int idx = fqcn != null ? fqcn.lastIndexOf('.') : -1;
        return idx >= 0 ? fqcn.substring(idx + 1) : (fqcn != null ? fqcn : "");
    }
}


