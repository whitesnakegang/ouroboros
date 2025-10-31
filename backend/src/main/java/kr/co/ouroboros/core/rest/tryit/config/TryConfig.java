package kr.co.ouroboros.core.rest.tryit.config;

import kr.co.ouroboros.core.rest.tryit.tracing.config.MethodTracingProperties;
import kr.co.ouroboros.core.rest.tryit.tempo.config.TempoProperties;
import kr.co.ouroboros.core.rest.tryit.tracing.processor.TrySpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Try session management configuration.
 * Enables TrySessionProperties and TempoProperties, along with scheduling.
 * 
 * Auto-configuration for Ouroboros SDK.
 * This configuration is automatically detected by Spring Boot when the SDK is on the classpath.
 */
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
     * @return TrySpanProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean
    public SpanProcessor trySpanProcessor() {
        return new TrySpanProcessor();
    }
    
}
