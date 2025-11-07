package kr.co.ouroboros.core.rest.tryit.config;

import io.opentelemetry.sdk.trace.SpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.config.MethodTracingProperties;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.processor.InMemoryTrySpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.processor.TempoTrySpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.config.TempoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

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
 *   <li>{@link SpanProcessor} - TempoTrySpanProcessor or InMemoryTrySpanProcessor based on Tempo enabled status</li>
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
     * Register a TempoTrySpanProcessor bean that enriches OpenTelemetry spans with a `tryId` attribute.
     * <p>
     * This bean is used when Tempo is enabled. It only adds tryId attributes to spans
     * but does not collect them in memory. Spans are automatically exported to Tempo.
     *
     * @return the created TempoTrySpanProcessor instance as a `SpanProcessor`
     */
    @Bean
    @ConditionalOnMissingBean(name = "trySpanProcessor")
    @ConditionalOnProperty(name = "ouroboros.tempo.enabled", havingValue = "true", matchIfMissing = false)
    public SpanProcessor trySpanProcessor() {
        log.info("Creating TempoTrySpanProcessor bean (Tempo enabled)");
        TempoTrySpanProcessor processor = new TempoTrySpanProcessor();
        log.info("TempoTrySpanProcessor bean created successfully");
        return processor;
    }
    
    /**
     * Register an InMemoryTrySpanProcessor bean that collects spans in memory.
     * <p>
     * This bean is used when Tempo is disabled. It adds tryId attributes to spans
     * and collects them in InMemoryTraceStorage for later retrieval.
     *
     * @param traceStorage The trace storage (in-memory when Tempo is disabled)
     * @return the created InMemoryTrySpanProcessor instance as a `SpanProcessor`
     */
    @Bean
    @ConditionalOnMissingBean(name = "trySpanProcessor")
    @ConditionalOnProperty(name = "ouroboros.tempo.enabled", havingValue = "false", matchIfMissing = true)
    public SpanProcessor inMemoryTrySpanProcessor(TraceStorage traceStorage) {
        log.info("Creating InMemoryTrySpanProcessor bean (Tempo disabled)");
        InMemoryTrySpanProcessor processor = new InMemoryTrySpanProcessor(traceStorage);
        log.info("InMemoryTrySpanProcessor bean created successfully");
        return processor;
    }
    
}