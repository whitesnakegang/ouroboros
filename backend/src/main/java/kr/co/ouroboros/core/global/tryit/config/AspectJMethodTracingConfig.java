package kr.co.ouroboros.core.global.tryit.config;

import io.micrometer.observation.ObservationRegistry;
import kr.co.ouroboros.core.global.tryit.config.properties.MethodTracingProperties;
import kr.co.ouroboros.core.global.tryit.infrastructure.instrumentation.aspect.MethodTracingAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Auto-configuration for AspectJ compile-time weaving based method tracing.
 * <p>
 * This configuration sets up AspectJ-based method tracing for classes in allowed packages,
 * creating OpenTelemetry spans for method invocations automatically using AspectJ
 * compile-time weaving instead of Spring's proxy mechanism.
 * <p>
 * <b>Differences from Spring AOP Mode:</b>
 * <ul>
 *   <li>Traces self-invocation (same class method calls)</li>
 *   <li>Traces private methods</li>
 *   <li>Traces static methods</li>
 *   <li>No proxy overhead at runtime</li>
 *   <li>Direct bytecode modification at compile-time</li>
 *   <li>Requires AspectJ weaving plugin configuration in user project build</li>
 * </ul>
 * <p>
 * <b>Activation:</b>
 * <ul>
 *   <li>Set {@code ouroboros.method-tracing.mode=ASPECTJ}</li>
 *   <li>Set {@code ouroboros.method-tracing.enabled=true}</li>
 *   <li>Configure {@code ouroboros.method-tracing.allowed-packages}</li>
 *   <li>Setup AspectJ weaving in build.gradle or pom.xml</li>
 * </ul>
 * <p>
 * <b>Prerequisites:</b>
 * <ul>
 *   <li>AspectJ weaving plugin configured in user project</li>
 *   <li>Ouroboros library JAR on aspectpath or inpath</li>
 *   <li>User application classes woven at compile-time</li>
 * </ul>
 * <p>
 * <b>Configuration Example:</b>
 * <pre>{@code
 * ouroboros.method-tracing.enabled=true
 * ouroboros.method-tracing.mode=ASPECTJ
 * ouroboros.method-tracing.allowed-packages[0]=com.example.service
 * ouroboros.method-tracing.allowed-packages[1]=com.example.repository
 * }</pre>
 * <p>
 * For detailed setup instructions, see the AspectJ Setup Guide in the documentation.
 *
 * @see MethodTracingAspect
 * @see MethodTracingProperties
 * @see MethodTracingConfig
 * @author Ouroboros Team
 * @since 1.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "ouroboros", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "ouroboros.method-tracing", name = "mode", havingValue = "ASPECTJ")
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@EnableConfigurationProperties(MethodTracingProperties.class)
public class AspectJMethodTracingConfig implements org.springframework.beans.factory.InitializingBean {

    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;
    private final MethodTracingProperties properties;

    public AspectJMethodTracingConfig(
            ObjectProvider<ObservationRegistry> observationRegistryProvider,
            MethodTracingProperties properties
    ) {
        this.observationRegistryProvider = observationRegistryProvider;
        this.properties = properties;
    }

    /**
     * Configures the AspectJ-based method tracing aspect after bean initialization.
     * <p>
     * This method does NOT create a Spring bean for the aspect. Instead, it retrieves
     * the AspectJ singleton instance via {@code aspectOf()} and injects Spring
     * dependencies into it.
     * <p>
     * The aspect is configured with:
     * <ul>
     *   <li>ObservationRegistry for creating observation spans</li>
     *   <li>MethodTracingProperties for allowed packages configuration</li>
     * </ul>
     * <p>
     * <b>Important:</b> In AspectJ CTW mode, the aspect instance is managed by AspectJ,
     * not Spring. The actual weaving happens at compile-time via the AspectJ compiler
     * configured in the user's build system.
     */
    @Override
    public void afterPropertiesSet() {
        log.info("╔════════════════════════════════════════════════════════════════════════╗");
        log.info("║ AspectJ Compile-Time Weaving Mode Enabled for Method Tracing          ║");
        log.info("╚════════════════════════════════════════════════════════════════════════╝");
        log.info("");
        log.info("✓ Mode: ASPECTJ (Compile-Time Weaving)");
        log.info("✓ Capabilities:");
        log.info("  - Traces self-invocation (same class method calls)");
        log.info("  - Traces private methods");
        log.info("  - Traces static methods");
        log.info("  - No proxy overhead at runtime");
        log.info("");

        if (properties.getAllowedPackages() == null || properties.getAllowedPackages().isEmpty()) {
            log.warn("⚠ WARNING: No allowed packages configured!");
            log.warn("  Method tracing will not capture any methods.");
            log.warn("  Configure 'ouroboros.method-tracing.allowed-packages' to enable method tracing.");
            log.warn("");
            log.warn("  Example:");
            log.warn("    ouroboros.method-tracing.allowed-packages[0]=com.example.service");
            log.warn("    ouroboros.method-tracing.allowed-packages[1]=com.example.repository");
        } else {
            log.info("✓ Allowed packages configured:");
            for (String pkg : properties.getAllowedPackages()) {
                log.info("  - {}", pkg);
            }
        }

        log.info("");
        log.info("⚡ IMPORTANT: Ensure AspectJ weaving is configured in your build system!");
        log.info("   For Gradle: Add 'io.freefair.aspectj.post-compile-weaving' plugin");
        log.info("   For Maven: Add 'aspectj-maven-plugin'");
        log.info("   See docs/ASPECTJ_SETUP.md for detailed setup instructions");
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Get the AspectJ singleton instance via reflection
        // (aspectOf() method is generated by AspectJ weaving at compile-time)
        MethodTracingAspect aspect;
        try {
            // Get the AspectJ singleton via aspectOf()
            java.lang.reflect.Method aspectOfMethod = MethodTracingAspect.class.getMethod("aspectOf");
            aspect = (MethodTracingAspect) aspectOfMethod.invoke(null);
            log.info("✓ Successfully retrieved AspectJ singleton instance via aspectOf()");
        } catch (NoSuchMethodException e) {
            String errorMessage = "AspectJ weaving is not configured in your build system.\n" +
                    "The aspectOf() method was not found on MethodTracingAspect.\n" +
                    "This means the AspectJ weaving plugin is missing or not properly configured.\n\n" +
                    "Please add the AspectJ weaving plugin to your build.gradle or pom.xml:\n\n" +
                    "For Gradle:\n" +
                    "  plugins {\n" +
                    "    id 'io.freefair.aspectj.post-compile-weaving' version '8.11'\n" +
                    "  }\n" +
                    "  dependencies {\n" +
                    "    aspect 'io.github.whitesnakegang:ouroboros:" + getClass().getPackage().getImplementationVersion() + "'\n" +
                    "    aspect 'org.aspectj:aspectjweaver:1.9.22'\n" +
                    "  }\n\n" +
                    "For Maven:\n" +
                    "  See docs/ASPECTJ_SETUP.md for detailed setup instructions.\n\n" +
                    "Without AspectJ weaving, method tracing will NOT work.";
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve AspectJ singleton instance via aspectOf()", e);
        }

        // Inject Spring dependencies
        aspect.setObservationRegistryProvider(observationRegistryProvider);
        aspect.setProperties(properties);
    }
}
