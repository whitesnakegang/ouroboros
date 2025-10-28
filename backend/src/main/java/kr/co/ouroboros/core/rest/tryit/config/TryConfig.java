package kr.co.ouroboros.core.rest.tryit.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Try session management configuration.
 * Enables TrySessionProperties and TempoProperties, along with scheduling.
 * 
 * Auto-configuration for Ouroboros SDK.
 * This configuration is automatically detected by Spring Boot when the SDK is on the classpath.
 */
@AutoConfiguration
@EnableConfigurationProperties({
        TrySessionProperties.class,
        TempoProperties.class
})
@ComponentScan(basePackages = "kr.co.ouroboros")
@EnableScheduling
public class TryConfig {
}
