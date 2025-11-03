package kr.co.ouroboros.core.rest.tryit.config;

import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingProperties;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.config.TempoProperties;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor.TrySpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.opentelemetry.sdk.trace.SpanProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Try feature configuration.
 * <p>
 * Auto-configuration for Ouroboros SDK Try feature.
 * This configuration is automatically detected by Spring Boot when the SDK is on the classpath.
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li>{@link TempoProperties} - Tempo storage configuration</li>
 *   <li>{@link MethodTracingProperties} - Method tracing configuration</li>
 * </ul>
 * <p>
 * <b>Beans:</b>
 * <ul>
 *   <li>{@link SpanProcessor} - TrySpanProcessor for adding tryId to spans</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        TempoProperties.class,
        MethodTracingProperties.class
})
@ComponentScan(basePackages = "kr.co.ouroboros")
@EnableScheduling
public class TryConfig {
    
    /**
     * Registers the TrySpanProcessor to automatically add tryId attributes to spans.
     * <p>
     * Spring Boot's Micrometer Tracing auto-configuration will automatically collect
     * all SpanProcessor beans and register them with OpenTelemetry SDK.
     * <p>
     * The TrySpanProcessor adds the tryId attribute to spans based on the
     * X-Ouroboros-Try header or tryId from the Try context.
     *
     * @return TrySpanProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "trySpanProcessor")
    public SpanProcessor trySpanProcessor() {
        log.info("Creating TrySpanProcessor bean");
        TrySpanProcessor processor = new TrySpanProcessor();
        log.info("TrySpanProcessor bean created successfully");
        return processor;
    }
    
}
