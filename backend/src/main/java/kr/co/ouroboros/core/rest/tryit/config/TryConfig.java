package kr.co.ouroboros.core.rest.tryit.config;

import kr.co.ouroboros.core.rest.tryit.span.TrySpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
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
        TempoProperties.class
})
@ComponentScan(basePackages = "kr.co.ouroboros")
@EnableScheduling
public class TryConfig {
    
    /**
     * Provides Tracer bean for TryFilter.
     * 
     * @param openTelemetry OpenTelemetry instance
     * @return Tracer instance
     */
    @Bean
    @ConditionalOnMissingBean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("ouroboros-try-filter");
    }
    
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
