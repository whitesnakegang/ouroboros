package kr.co.ouroboros.core.rest.tryit.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Tempo (distributed tracing backend).
 * <p>
 * This class holds configuration properties for Tempo integration,
 * including connection settings, timeout, and polling configuration.
 * <p>
 * <b>Configuration Properties:</b>
 * <ul>
 *   <li>{@code ouroboros.tempo.enabled} - Enable/disable Tempo integration (default: false)</li>
 *   <li>{@code ouroboros.tempo.base-url} - Tempo base URL (default: http://localhost:3200)</li>
 *   <li>{@code ouroboros.tempo.query-timeout-seconds} - Query timeout in seconds (default: 30)</li>
 *   <li>{@code ouroboros.tempo.poll-interval-millis} - Poll interval in milliseconds (default: 1000)</li>
 *   <li>{@code ouroboros.tempo.max-poll-attempts} - Maximum poll attempts (default: 10)</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * ouroboros.tempo.enabled=true
 * ouroboros.tempo.base-url=http://tempo:3200
 * ouroboros.tempo.query-timeout-seconds=30
 * ouroboros.tempo.poll-interval-millis=1000
 * ouroboros.tempo.max-poll-attempts=10
 * }</pre>
 * <p>
 * Configured via {@code ouroboros.tempo.*} prefix in application.properties.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "ouroboros.tempo")
public class TempoProperties {
    
    /**
     * Whether Tempo integration is enabled.
     * <p>
     * When disabled, all Tempo operations (search, fetch, poll) will return
     * empty results or null without making HTTP requests.
     * <p>
     * Default: false (for SDK library - actual usage should override)
     */
    private boolean enabled = false;
    
    /**
     * Tempo base URL.
     * <p>
     * Base URL for Tempo API endpoints. Should not include trailing slash.
     * <p>
     * Example: http://localhost:3200 or http://tempo:3200
     * <p>
     * Default: http://localhost:3200
     */
    private String baseUrl = "http://localhost:3200";
    
    /**
     * Query timeout in seconds for HTTP requests to Tempo.
     * <p>
     * Maximum time to wait for a response from Tempo API.
     * Used as read timeout for RestTemplate.
     * <p>
     * Default: 30 seconds
     */
    private int queryTimeoutSeconds = 30;
    
    /**
     * Poll interval in milliseconds.
     * <p>
     * How long to wait between polling attempts when using
     * {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient#pollForTrace(String)}.
     * <p>
     * Total maximum wait time = {@code maxPollAttempts * pollIntervalMillis}
     * <p>
     * Default: 1000ms (1 second)
     */
    private long pollIntervalMillis = 1000;
    
    /**
     * Maximum number of polling attempts.
     * <p>
     * Maximum number of attempts when polling for traces.
     * Combined with {@code pollIntervalMillis}, determines maximum wait time:
     * <pre>maxWaitTime = maxPollAttempts * pollIntervalMillis</pre>
     * <p>
     * Example: 10 attempts * 1000ms = 10 seconds maximum wait time
     * <p>
     * Default: 10 attempts
     */
    private int maxPollAttempts = 10;
}

