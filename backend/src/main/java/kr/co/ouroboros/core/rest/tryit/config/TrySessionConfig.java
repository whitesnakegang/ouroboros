package kr.co.ouroboros.core.rest.tryit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Try session management configuration.
 * Enables TrySessionProperties and TempoProperties, along with scheduling.
 */
@Configuration
@EnableConfigurationProperties({
        TrySessionProperties.class,
        TempoProperties.class
})
@EnableScheduling
public class TrySessionConfig {
}
