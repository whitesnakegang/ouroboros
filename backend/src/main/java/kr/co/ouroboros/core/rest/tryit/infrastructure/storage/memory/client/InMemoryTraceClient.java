package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.InMemoryTraceStorage;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-memory implementation of TraceClient interface.
 * <p>
 * This client provides trace querying capabilities using in-memory storage
 * instead of an external backend like Tempo.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Implements TraceClient interface for compatibility</li>
 *   <li>Retrieves traces from InMemoryTraceStorage</li>
 *   <li>Supports TraceQL query parsing (simple tryId extraction)</li>
 *   <li>No polling delay (traces are immediately available)</li>
 * </ul>
 * <p>
 * This client is used when Tempo is disabled (ouroboros.tempo.enabled=false).
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "ouroboros.tempo.enabled", 
        havingValue = "false", 
        matchIfMissing = true
)
@org.springframework.context.annotation.Primary
@RequiredArgsConstructor
public class InMemoryTraceClient implements TraceClient {
    
    private final InMemoryTraceStorage traceStorage;
    private final ObjectMapper objectMapper;
    
    /**
     * Pattern to extract tryId from TraceQL query: { span.ouro.try_id = "tryId" }
     */
    private static final Pattern TRY_ID_PATTERN = Pattern.compile(
            "span\\.ouro\\.try_id\\s*=\\s*\"([^\"]+)\""
    );
    
    /**
     * Searches for traces matching the given query.
     * <p>
     * Currently only supports simple tryId queries: { span.ouro.try_id = "tryId" }
     *
     * @param query Query string (TraceQL format for compatibility)
     * @return List of trace IDs matching the query
     */
    @Override
    public List<String> searchTraces(String query) {
        String tryId = extractTryIdFromQuery(query);
        if (tryId == null) {
            log.debug("Could not extract tryId from query: {}", query);
            return Collections.emptyList();
        }
        
        String traceId = traceStorage.getTraceId(tryId);
        if (traceId == null) {
            log.debug("Trace not found for tryId: {}", tryId);
            return Collections.emptyList();
        }
        
        log.debug("Found trace in memory: tryId={}, traceId={}", tryId, traceId);
        return List.of(traceId);
    }
    
    /**
     * Retrieves the full trace data for a given trace ID.
     *
     * @param traceId The trace ID to fetch
     * @return Trace data as JSON string, or null if not found
     */
    @Override
    public String getTrace(String traceId) {
        TraceDTO traceData = traceStorage.getTraceByTraceId(traceId);
        if (traceData == null) {
            log.debug("Trace not found in memory: traceId={}", traceId);
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(traceData);
            log.debug("Retrieved trace from memory: traceId={}", traceId);
            return json;
        } catch (Exception e) {
            log.error("Failed to serialize trace to JSON: traceId={}", traceId, e);
            return null;
        }
    }
    
    /**
     * Polls for a trace matching the given query.
     * <p>
     * For in-memory storage, traces are immediately available, so this
     * method simply calls searchTraces and returns the first result.
     *
     * @param query Query string (TraceQL format for compatibility)
     * @return The found trace ID, or null if not found
     */
    @Override
    public String pollForTrace(String query) {
        List<String> traces = searchTraces(query);
        if (traces.isEmpty()) {
            return null;
        }
        return traces.get(0);
    }
    
    /**
     * Checks whether this client is enabled.
     * <p>
     * This client is always enabled when Tempo is disabled.
     *
     * @return true (always enabled for in-memory storage)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * Extracts tryId from TraceQL query string.
     * <p>
     * Supports format: { span.ouro.try_id = "tryId" }
     *
     * @param query TraceQL query string
     * @return Extracted tryId, or null if not found
     */
    private String extractTryIdFromQuery(String query) {
        if (query == null) {
            return null;
        }
        
        Matcher matcher = TRY_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}

