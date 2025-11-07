package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.config.properties.TempoProperties;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RestTemplate-based implementation of TraceClient for Tempo backend.
 * <p>
 * This component provides HTTP client implementation for interacting with Tempo
 * (distributed tracing backend) using Spring's RestTemplate.
 * <p>
 * <b>Tempo API Endpoints:</b>
 * <ul>
 *   <li>GET /api/search?q={traceql} - Search traces using TraceQL</li>
 *   <li>GET /api/traces/{traceId} - Get trace data by trace ID</li>
 * </ul>
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Configurable timeout from TempoProperties</li>
 *   <li>Automatic query parameter encoding</li>
 *   <li>Polling support with configurable interval and max attempts</li>
 *   <li>JSON response parsing</li>
 * </ul>
 * <p>
 * Configuration is provided via {@link kr.co.ouroboros.core.rest.tryit.config.properties.TempoProperties}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "ouroboros.tempo.enabled", 
        havingValue = "true", 
        matchIfMissing = false
)
public class RestTemplateTempoClient implements TraceClient {
    
    private final TempoProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a RestTemplateTempoClient configured with the provided TempoProperties and RestTemplateBuilder.
     *
     * Configures the underlying RestTemplate with a 5-second connect timeout and a read timeout taken from
     * the properties' queryTimeoutSeconds.
     *
     * @param properties Tempo configuration properties used to obtain the query timeout
     * @param builder RestTemplateBuilder used to build the configured RestTemplate
     */
    public RestTemplateTempoClient(TempoProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(properties.getQueryTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Search Tempo for traces matching the given TraceQL query and return their trace IDs.
     *
     * @param query TraceQL query string (for example: "{ span.ouro.try_id = \"tryId\" }")
     * @return List of trace IDs matching the query; empty list if Tempo is disabled, the request fails, or no traces match
     */
    @Override
    public List<String> searchTraces(String query) {
        if (!isEnabled()) {
            log.debug("Tempo is disabled, skipping trace search");
            return Collections.emptyList();
        }
        
        try {
            // Manually encode query parameter to avoid URI template variable expansion
            String encodedQuery = UriUtils.encode(query, StandardCharsets.UTF_8);
            String url = properties.getBaseUrl() + "/api/search?q=" + encodedQuery;
            log.debug("Searching Tempo: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET, 
                    entity, 
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseTraceIds(response.getBody());
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to search Tempo: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Retrieve the full trace for a given trace ID from Tempo.
     *
     * @return the trace as a JSON string, or `null` if Tempo is disabled, the trace is not found, or an error occurs
     */
    @Override
    public String getTrace(String traceId) {
        if (!isEnabled()) {
            log.debug("Tempo is disabled, skipping trace fetch");
            return null;
        }
        
        try {
            String url = properties.getBaseUrl() + "/api/traces/" + traceId;
            log.debug("Fetching trace from Tempo: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch trace from Tempo: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Polls Tempo for a trace matching the given TraceQL query until one is found or polling attempts are exhausted.
     *
     * @param query TraceQL query string (for example, "{ span.ouro.try_id = \"tryId\" }")
     * @return the first matching trace ID if found; `null` if no trace is found within the configured polling attempts or if polling is interrupted/errors occur
     */
    @Override
    public String pollForTrace(String query) {
        if (!isEnabled()) {
            log.debug("Tempo is disabled, skipping trace polling");
            return null;
        }
        
        for (int attempt = 0; attempt < properties.getMaxPollAttempts(); attempt++) {
            List<String> traces = searchTraces(query);
            
            if (!traces.isEmpty()) {
                log.debug("Found trace on attempt {}: {}", attempt + 1, traces.get(0));
                return traces.get(0);
            }
            
            if (attempt < properties.getMaxPollAttempts() - 1) {
                try {
                    Thread.sleep(properties.getPollIntervalMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.debug("Trace not found after {} attempts", properties.getMaxPollAttempts());
        return null;
    }
    
    /**
     * Indicates whether Tempo integration is enabled.
     *
     * @return `true` if enabled, `false` otherwise
     */
    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * Extracts trace IDs from a Tempo search API JSON response.
     *
     * <p>Expects a JSON object containing a "traces" array whose elements include a
     * "traceID" string field (e.g. {@code {"traces":[{"traceID":"..."}]}}). Returns
     * an empty list when the expected structure is missing, no trace IDs are present,
     * or parsing fails.
     *
     * @param jsonResponse JSON response string from the Tempo search API
     * @return a list of trace IDs found in the response, or an empty list if none
     */
    private List<String> parseTraceIds(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode tracesNode = root.get("traces");
            
            if (tracesNode == null || !tracesNode.isArray()) {
                return Collections.emptyList();
            }
            
            List<String> traceIds = new ArrayList<>();
            for (JsonNode trace : tracesNode) {
                JsonNode id = trace.get("traceID");
                if (id != null && id.isTextual()) {
                    traceIds.add(id.asText());
                }
            }
            
            return traceIds;
        } catch (Exception e) {
            log.warn("Failed to parse Tempo response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
