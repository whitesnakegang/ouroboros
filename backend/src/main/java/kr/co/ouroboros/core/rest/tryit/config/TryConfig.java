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
 * Try session management configuration.
 * Enables TrySessionProperties and TempoProperties, along with scheduling.
 * 
 * Auto-configuration for Ouroboros SDK.
 * This configuration is automatically detected by Spring Boot when the SDK is on the classpath.
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
     * 
     * Spring Boot's Micrometer Tracing auto-configuration will automatically collect
     * all SpanProcessor beans and register them with OpenTelemetry SDK.
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
