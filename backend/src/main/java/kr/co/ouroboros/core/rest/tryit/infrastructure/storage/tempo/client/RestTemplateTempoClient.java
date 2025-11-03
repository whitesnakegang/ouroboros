package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.config.TempoProperties;
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
 * RestTemplate-based implementation of TempoClient.
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
 * Configuration is provided via {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.config.TempoProperties}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class RestTemplateTempoClient implements TempoClient {
    
    private final TempoProperties properties;
    private final RestTemplate restTemplate;
    
    /**
     * Constructs RestTemplateTempoClient with configuration properties and RestTemplate builder.
     * <p>
     * Configures RestTemplate with:
     * <ul>
     *   <li>Connect timeout: 5 seconds</li>
     *   <li>Read timeout: from TempoProperties.queryTimeoutSeconds</li>
     * </ul>
     *
     * @param properties Tempo configuration properties
     * @param builder RestTemplate builder for creating configured RestTemplate
     */
    public RestTemplateTempoClient(TempoProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(properties.getQueryTimeoutSeconds()))
                .build();
    }
    
    /**
     * Searches for traces using TraceQL query.
     * <p>
     * Executes a TraceQL query against Tempo search API and parses
     * the response to extract trace IDs.
     * <p>
     * Returns empty list if:
     * <ul>
     *   <li>Tempo is disabled</li>
     *   <li>Request fails</li>
     *   <li>No traces match the query</li>
     * </ul>
     *
     * @param query TraceQL query string (e.g., "{ span.ouro.try_id = \"tryId\" }")
     * @return List of trace IDs matching the query, empty list if none found or error occurs
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
     * Fetches trace data by trace ID.
     * <p>
     * Retrieves full trace data from Tempo for the specified trace ID.
     * The returned data is in JSON format as returned by Tempo API.
     * <p>
     * Returns null if:
     * <ul>
     *   <li>Tempo is disabled</li>
     *   <li>Request fails</li>
     *   <li>Trace not found</li>
     * </ul>
     *
     * @param traceId the trace ID to fetch
     * @return Trace data in JSON format, or null if not found or error occurs
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
     * Polls for traces matching the query until found or timeout.
     * <p>
     * Repeatedly searches for traces matching the query with configurable
     * polling interval until a trace is found or maximum attempts are reached.
     * <p>
     * <b>Polling Configuration:</b>
     * <ul>
     *   <li>Poll interval: from {@link TempoProperties#pollIntervalMillis}</li>
     *   <li>Max attempts: from {@link TempoProperties#maxPollAttempts}</li>
     * </ul>
     * <p>
     * Useful for waiting for traces that may not be immediately available in Tempo.
     * Thread sleep is used between attempts, with interrupt handling.
     *
     * @param query TraceQL query string (e.g., "{ span.ouro.try_id = \"tryId\" }")
     * @return Trace ID if found within max attempts, null if timeout or error occurs
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
     * Checks if Tempo is enabled and available.
     * <p>
     * Delegates to {@link TempoProperties#enabled} field.
     *
     * @return true if Tempo is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * Parses trace IDs from Tempo search response.
     * <p>
     * Parses JSON response from Tempo search API and extracts trace IDs.
     * Expected format: {@code {"traces": [{"traceID": "...", ...}, ...]}}
     * <p>
     * Returns empty list if:
     * <ul>
     *   <li>Response format is invalid</li>
     *   <li>Parsing fails</li>
     *   <li>No traces found in response</li>
     * </ul>
     *
     * @param jsonResponse JSON response string from Tempo search API
     * @return List of trace IDs extracted from response, empty list if parsing fails
     */
    private List<String> parseTraceIds(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
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

