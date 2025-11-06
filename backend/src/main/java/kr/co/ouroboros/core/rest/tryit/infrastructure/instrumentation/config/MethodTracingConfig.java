package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config;

import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect.MethodTracingAspect;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.aspect.MethodTracingMethodInterceptor;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Auto-configuration for method-level tracing using Spring AOP.
 * <p>
 * This configuration sets up AOP-based method tracing for classes in allowed packages,
 * creating OpenTelemetry spans for method invocations automatically.
 * <p>
 * <b>Note:</b> Spring AOP has limitations - it doesn't track self-invocations (methods called
 * within the same class). For complete method tracing including self-invocations, users should
 * configure AspectJ in their own project.
 * <p>
 * <b>Configuration:</b>
 * <ul>
 *   <li>Uses {@link MethodTracingProperties} for configuration</li>
 *   <li>Creates AOP advisor that intercepts methods in allowed packages</li>
 *   <li>Excludes SDK classes (kr.co.ouroboros.*) from tracing</li>
 *   <li>Checks class, interfaces, and superclass chain for allowed packages</li>
 * </ul>
 * <p>
 * <b>Class Filtering:</b>
 * <ul>
 *   <li>Target class itself is in allowed package</li>
 *   <li>Implements interface in allowed package (e.g., Spring Data JPA Repository)</li>
 *   <li>Extends superclass in allowed package</li>
 *   <li>Always excludes kr.co.ouroboros.* SDK classes</li>
 * </ul>
 * <p>
 * If no allowed packages are configured, method tracing is disabled.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@AutoConfiguration
@EnableConfigurationProperties(MethodTracingProperties.class)
@ConditionalOnClass({Advisor.class, org.aspectj.lang.ProceedingJoinPoint.class})
public class MethodTracingConfig {

    /**
     * Creates an AOP advisor that applies method-level tracing to application classes using Spring AOP.
     * <p>
     * The returned advisor applies MethodTracingMethodInterceptor to methods of classes whose packages are allowed by
     * MethodTracingProperties; if no allowed packages are configured the advisor matches no classes (tracing disabled).
     * <p>
     * <b>Note:</b> This advisor is only created when AspectJ is NOT available. When AspectJ is available,
     * the AspectJ aspect takes precedence.
     *
     * @param observationRegistryProvider optional provider for ObservationRegistry used by the interceptor
     * @param props configuration properties containing allowed packages for tracing
     * @param beanFactory reserved for future use
     * @return an Advisor that instruments methods in configured allowed packages using MethodTracingMethodInterceptor
     */
    @Bean
    @ConditionalOnMissingBean(name = "ouroborosMethodTracingAdvisor")
    @ConditionalOnMissingClass("org.aspectj.lang.ProceedingJoinPoint")
    public Advisor ouroborosMethodTracingAdvisor(
            ObjectProvider<io.micrometer.observation.ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties props,
            BeanFactory beanFactory
    ) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodTracingConfig.class);
        log.info("🚀 Creating Spring AOP advisor - AspectJ NOT detected, using Spring AOP");
        log.info("   Note: Spring AOP cannot track self-invocations");
        // allowlist 루트 패키지 수집: properties.allowedPackages가 우선
        final Set<String> roots = new HashSet<>();
        if (props.getAllowedPackages() != null && !props.getAllowedPackages().isEmpty()) {
            roots.addAll(props.getAllowedPackages());
        }

        // ClassFilter: 사용자 루트 패키지 하위만 허용, SDK/인프라/자바/OTel/Micrometer 등은 제외
        ClassFilter classFilter = clazz -> {
            if (!props.isEnabled()) return false;
            // 허용 목록이 비어있으면 관찰 비활성화
            if (roots.isEmpty()) return false;

            // final 클래스는 프록시 생성 불가능하므로 제외
            if (java.lang.reflect.Modifier.isFinal(clazz.getModifiers())) {
                return false;
            }

            // SDK 자체는 제외
            if (clazz.getName().startsWith("kr.co.ouroboros.")) return false;

            // 1) 대상 클래스 자체가 허용 패키지인지
            for (String root : roots) {
                String className = clazz.getName();
                if (className.equals(root) || className.startsWith(root + ".")) return true;
            }

            // 2) 구현 인터페이스 중 허용 패키지에 속하는 것이 있는지 (Spring Data JPA Repository 등)
            Class<?>[] ifaces = clazz.getInterfaces();
            for (Class<?> iface : ifaces) {
                String in = iface.getName();
                for (String root : roots) {
                    if (in.equals(root) || in.startsWith(root + ".")) return true;
                }
            }

            // 3) 슈퍼클래스 체인에도 사용자 클래스가 있는지 확인 (프록시/프레임워크 래퍼 대비)
            Class<?> sc = clazz.getSuperclass();
            while (sc != null && sc != Object.class) {
                String sn = sc.getName();
                if (sn.startsWith("kr.co.ouroboros.")) return false; // SDK 제외 우선
                for (String root : roots) {
                    if (sn.equals(root) || sn.startsWith(root + ".")) return true;
                }
                sc = sc.getSuperclass();
            }

            return false;
        };

        // 모든 메서드 허용(클래스 필터로만 제한)
        Pointcut pointcut = new StaticMethodMatcherPointcut() {
            /**
             * Matches every method unconditionally so method-level filtering is disabled.
             *
             * @param method the method being evaluated
             * @param targetClass the class on which the method is declared or invoked
             * @return true indicating this pointcut matches all methods
             */
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return true;
            }

            /**
             * Provide the ClassFilter used by the pointcut to determine which classes are eligible for method tracing.
             *
             * @return the ClassFilter that enforces configured allowed package roots and excludes SDK/infrastructure packages
             */
            @Override
            public ClassFilter getClassFilter() {
                return classFilter;
            }
        };

        Advice advice = new MethodTracingMethodInterceptor(observationRegistryProvider, props);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }

    /**
     * Creates an AspectJ Aspect that applies method-level tracing to application classes.
     * <p>
     * <b>IMPORTANT:</b> This bean is created for Load-Time Weaving (LTW) support.
     * For compile-time weaving, AspectJ will find the aspect via aop.xml, not via Spring Bean.
     * <p>
     * This bean is only created when AspectJ classes are available on the classpath.
     * When AspectJ is available, this aspect takes precedence over Spring AOP advisor
     * because AspectJ can track self-invocations.
     * <p>
     * <b>Note:</b> For compile-time weaving, users must:
     * <ul>
     *   <li>Add the AspectJ Gradle/Maven plugin</li>
     *   <li>Create aop.xml file in src/main/resources/META-INF/</li>
     *   <li>Do NOT rely on this Spring Bean (AspectJ will find the aspect via aop.xml)</li>
     * </ul>
     * <p>
     * For Load-Time Weaving (LTW), users must:
     * <ul>
     *   <li>Add -javaagent:aspectjweaver.jar to JVM arguments</li>
     *   <li>Create aop.xml file in src/main/resources/META-INF/</li>
     *   <li>This Spring Bean will be used to initialize the aspect with properties</li>
     * </ul>
     *
     * @param observationRegistryProvider optional provider for ObservationRegistry used by the aspect
     * @param props configuration properties containing allowed packages for tracing
     * @return a MethodTracingAspect that instruments methods in configured allowed packages
     */
    @Bean
    @ConditionalOnMissingBean(name = "ouroborosMethodTracingAspect")
    @ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
    public MethodTracingAspect ouroborosMethodTracingAspect(
            ObjectProvider<io.micrometer.observation.ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties props
    ) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodTracingConfig.class);
        log.info("🚀 Creating MethodTracingAspect bean - AspectJ Post-Compile Weaving (PCW) mode");
        log.info("   Note: PCW uses AspectJ instance from aop.xml, not Spring Bean instance");
        log.info("   This bean is created for dependency injection, but AspectJ will use its own instance");
        
        return new MethodTracingAspect(observationRegistryProvider, props);
    }
}
