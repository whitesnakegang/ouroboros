package kr.co.ouroboros.core.rest.tryit.config;

import kr.co.ouroboros.core.rest.tryit.aspect.MethodTracingMethodInterceptor;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@AutoConfiguration
@EnableConfigurationProperties(MethodTracingProperties.class)
@ConditionalOnClass(Advisor.class)
public class MethodTracingAdvisorConfig {

    @Bean
    @ConditionalOnMissingBean(name = "ouroborosMethodTracingAdvisor")
    public Advisor ouroborosMethodTracingAdvisor(
            ObjectProvider<io.micrometer.observation.ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties props,
            BeanFactory beanFactory
    ) {
        // allowlist 루트 패키지 수집: properties.allowedPackages가 우선
        final Set<String> roots = new HashSet<>();
        if (props.getAllowedPackages() != null && !props.getAllowedPackages().isEmpty()) {
            roots.addAll(props.getAllowedPackages());
        }

        // ClassFilter: 사용자 루트 패키지 하위만 허용, SDK/인프라/자바/OTel/Micrometer 등은 제외
        ClassFilter classFilter = clazz -> {
            // 허용 목록이 비어있으면 관찰 비활성화
            if (roots.isEmpty()) return false;

            // SDK 자체는 제외
            if (clazz.getName().startsWith("kr.co.ouroboros.")) return false;

            // 1) 대상 클래스 자체가 허용 패키지인지
            for (String root : roots) {
                if (clazz.getName().startsWith(root)) return true;
            }

            // 2) 구현 인터페이스 중 허용 패키지에 속하는 것이 있는지 (Spring Data JPA Repository 등)
            Class<?>[] ifaces = clazz.getInterfaces();
            for (Class<?> iface : ifaces) {
                String in = iface.getName();
                for (String root : roots) {
                    if (in.startsWith(root)) return true;
                }
            }

            // 3) 슈퍼클래스 체인에도 사용자 클래스가 있는지 확인 (프록시/프레임워크 래퍼 대비)
            Class<?> sc = clazz.getSuperclass();
            while (sc != null && sc != Object.class) {
                String sn = sc.getName();
                if (sn.startsWith("kr.co.ouroboros.")) return false; // SDK 제외 우선
                for (String root : roots) {
                    if (sn.startsWith(root)) return true;
                }
                sc = sc.getSuperclass();
            }

            return false;
        };

        // 모든 메서드 허용(클래스 필터로만 제한)
        Pointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return true;
            }

            @Override
            public ClassFilter getClassFilter() {
                return classFilter;
            }
        };

        Advice advice = new MethodTracingMethodInterceptor(observationRegistryProvider, props);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
}


