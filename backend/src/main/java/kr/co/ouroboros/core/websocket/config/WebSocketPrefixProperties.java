package kr.co.ouroboros.core.websocket.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.stereotype.Component;

/**
 * Automatically detects WebSocket/STOMP destination prefixes from Spring WebSocket configuration.
 * <p>
 * This class scans the {@link SimpAnnotationMethodMessageHandler} bean to extract
 * application destination prefixes configured in {@code WebSocketMessageBrokerConfigurer}.
 * <p>
 * This bean is only registered when WebSocket support is available on the classpath.
 * If WebSocket is not configured, this bean will not be created.
 * <p>
 * Fallback to default values if Spring WebSocket is configured but prefixes are not detected:
 * <ul>
 *   <li>Application destination prefix: {@code /app}</li>
 *   <li>Broker prefixes: {@code /topic}, {@code /queue}</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Slf4j
@Getter
@Component
@ConditionalOnClass(SimpAnnotationMethodMessageHandler.class)
public class WebSocketPrefixProperties {

    private String applicationDestinationPrefix = "/app";
    private List<String> brokerPrefixes = Arrays.asList("/topic", "/queue");

    @Autowired(required = false)
    private SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Automatically detect prefixes from Spring WebSocket configuration after bean initialization.
     * <p>
     * All detected prefixes are normalized by removing trailing slashes to ensure consistent
     * comparison in {@code hasKnownPrefix} and {@code ensurePrefix} methods.
     */
    @PostConstruct
    public void detectPrefixes() {
        // Detect application destination prefixes
        if (simpAnnotationMethodMessageHandler != null) {
            Collection<String> detectedPrefixes = simpAnnotationMethodMessageHandler.getDestinationPrefixes();
            if (detectedPrefixes != null && !detectedPrefixes.isEmpty()) {
                // Use the first prefix as the primary one
                applicationDestinationPrefix = detectedPrefixes.iterator().next();
                // Normalize: remove trailing slash for consistent comparison
                applicationDestinationPrefix = normalizePrefix(applicationDestinationPrefix);
                log.info("Detected application destination prefix from WebSocket config: {}", applicationDestinationPrefix);
            } else {
                log.warn("No application destination prefix detected, using default: {}", applicationDestinationPrefix);
            }
        } else {
            log.warn("SimpAnnotationMethodMessageHandler not found, using default application prefix: {}", applicationDestinationPrefix);
        }

        // Normalize broker prefixes (remove trailing slashes)
        brokerPrefixes = brokerPrefixes.stream()
                .map(this::normalizePrefix)
                .collect(Collectors.toList());

        // Detect broker prefixes (from messaging template's user destination prefix pattern)
        // Note: Broker prefixes are harder to detect, keeping defaults for now
        log.info("Using broker prefixes: {}", brokerPrefixes);
    }

    /**
     * Normalizes a prefix by removing trailing slash if present.
     * <p>
     * Ensures consistent prefix format for comparison in {@code hasKnownPrefix}.
     *
     * @param prefix the prefix to normalize
     * @return normalized prefix without trailing slash
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return prefix;
        }
        // Remove single trailing slash if present
        if (prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
