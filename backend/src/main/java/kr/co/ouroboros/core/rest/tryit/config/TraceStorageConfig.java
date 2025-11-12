package kr.co.ouroboros.core.rest.tryit.config;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import kr.co.ouroboros.core.rest.tryit.config.properties.TempoProperties;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.sampler.TryOnlySampler;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.processor.InMemoryTrySpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.processor.TempoTrySpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Trace storage configuration.
 * <p>
 * Auto-configuration for trace storage selection (Tempo or in-memory).
 * This configuration is automatically detected by Spring Boot when the SDK is on the classpath.
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li>{@link TempoProperties} - Tempo storage configuration</li>
 * </ul>
 * <p>
 * <b>Beans:</b>
 * <ul>
 *   <li>{@link Sampler} - TryOnlySampler to sample only Try requests</li>
 *   <li>{@link SpanProcessor} - TempoTrySpanProcessor or InMemoryTrySpanProcessor based on Tempo enabled status</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(OpenTelemetryTracingAutoConfiguration.class)
@EnableConfigurationProperties(TempoProperties.class)
public class TraceStorageConfig {
    
    /**
     * Register a TempoTrySpanProcessor bean that enriches OpenTelemetry spans with a `tryId` attribute.
     * <p>
     * This bean is used when Tempo is enabled. It only adds tryId attributes to spans
     * but does not collect them in memory. Spans are automatically exported to Tempo.
     *
     * @return the created TempoTrySpanProcessor instance as a `SpanProcessor`
     */
    @Bean(name = "trySpanProcessor")
    @ConditionalOnMissingBean(name = "trySpanProcessor")
    @ConditionalOnProperty(name = "ouroboros.tempo.enabled", havingValue = "true", matchIfMissing = false)
    public SpanProcessor tempoTrySpanProcessor() {
        log.info("Creating TempoTrySpanProcessor bean (Tempo enabled)");
        TempoTrySpanProcessor processor = new TempoTrySpanProcessor();
        log.info("TempoTrySpanProcessor bean created successfully");
        return processor;
    }
    
    /**
     * Provides an in-memory SpanProcessor used when Tempo is disabled.
     *
     * The processor attaches try identifiers to spans and stores them in the provided
     * TraceStorage for later retrieval.
     *
     * @param traceStorage the TraceStorage used to persist spans in memory
     * @return the created InMemoryTrySpanProcessor as a `SpanProcessor`
     */
    @Bean(name = "trySpanProcessor")
    @ConditionalOnMissingBean(name = "trySpanProcessor")
    @ConditionalOnProperty(name = "ouroboros.tempo.enabled", havingValue = "false", matchIfMissing = true)
    public SpanProcessor inMemoryTrySpanProcessor(TraceStorage traceStorage) {
        log.info("Creating InMemoryTrySpanProcessor bean (Tempo disabled)");
        InMemoryTrySpanProcessor processor = new InMemoryTrySpanProcessor(traceStorage);
        log.info("InMemoryTrySpanProcessor bean created successfully");
        return processor;
    }

    /**
     * Register TryOnlySampler bean to sample only Try requests.
     * <p>
     * This sampler prevents span creation for non-Try requests, reducing
     * tracing overhead and Tempo storage usage. Only requests with
     * X-Ouroboros-Try: on header will be traced.
     * <p>
     * <b>Important:</b> This bean does NOT use @ConditionalOnMissingBean to ensure
     * it always overrides Spring Boot's default Sampler. The property
     * management.tracing.sampling.probability is not needed and will be ignored.
     *
     * @return the TryOnlySampler instance
     */
    @Bean
    @Primary
    public Sampler sampler() {
        TryOnlySampler sampler = new TryOnlySampler();
        log.info("TryOnlySampler bean created successfully");
        return sampler;
    }

}