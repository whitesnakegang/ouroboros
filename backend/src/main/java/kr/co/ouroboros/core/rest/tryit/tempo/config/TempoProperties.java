package kr.co.ouroboros.core.rest.tryit.tempo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tempo configuration properties.
 * Configured via 'ouroboros.tempo.*' prefix in application.properties.
 */
@Data
@ConfigurationProperties(prefix = "ouroboros.tempo")
public class TempoProperties {
    
    /**
     * Whether Tempo integration is enabled.
     * Default false for SDK library (actual usage will override).
     */
    private boolean enabled = false;
    
    /**
     * Tempo base URL.
     * Example: http://localhost:3200
     */
    private String baseUrl = "http://localhost:3200";
    
    /**
     * Query timeout in seconds.
     * Default 30 seconds.
     */
    private int queryTimeoutSeconds = 30;
    
    /**
     * Poll interval in milliseconds.
     * How long to wait between polling attempts.
     * Default 1000ms (1 second).
     */
    private long pollIntervalMillis = 1000;
    
    /**
     * Maximum poll attempts.
     * Total wait time = maxPollAttempts * pollIntervalMillis.
     * Default 10 attempts (10 seconds).
     */
    private int maxPollAttempts = 10;
}

