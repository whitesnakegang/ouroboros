package kr.co.ouroboros.core.rest.tryit.tempo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.config.TempoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RestTemplate-based implementation of TempoClient.
 * 
 * Tempo API Endpoints:
 * - GET /api/search?q={traceql} - Search traces
 * - GET /api/traces/{traceId} - Get trace data
 */
@Slf4j
@Component
public class RestTemplateTempoClient implements TempoClient {
    
    private final TempoProperties properties;
    private final RestTemplate restTemplate;
    
    public RestTemplateTempoClient(TempoProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(properties.getQueryTimeoutSeconds()))
                .build();
    }
    
    @Override
    public List<String> searchTraces(String query) {
        if (!isEnabled()) {
            log.debug("Tempo is disabled, skipping trace search");
            return Collections.emptyList();
        }
        
        try {
            String url = properties.getBaseUrl() + "/api/search?q=" + query;
            log.debug("Searching Tempo: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, 
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
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * Parses trace IDs from Tempo search response.
     * Expected format: {"traces": [{"id": "...", ...}, ...]}
     * 
     * @param jsonResponse JSON response from Tempo
     * @return list of trace IDs
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
