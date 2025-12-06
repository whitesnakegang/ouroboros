package kr.co.ouroboros.core.global.tryit.infrastructure.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.global.tryit.config.properties.TempoProperties;
import kr.co.ouroboros.core.global.tryit.infrastructure.storage.memory.InMemoryTraceStorage;
import kr.co.ouroboros.core.global.tryit.infrastructure.storage.model.TraceDTO;
import kr.co.ouroboros.core.global.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.global.tryit.trace.dto.TraceSpanInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Component for retrieving and parsing trace data.
 * <p>
 * This component provides common functionality for trace retrieval and conversion
 * that is shared across multiple Try services. It handles trace querying,
 * parsing, and conversion to TraceSpanInfo.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Cache-first retrieval (Tempo mode only)</li>
 *   <li>Trace querying by tryId</li>
 *   <li>Trace data parsing from JSON</li>
 *   <li>TraceDTO to TraceSpanInfo conversion</li>
 *   <li>Trace client enabled check</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceDataRetriever {

    private final TraceClient traceClient;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final InMemoryTraceStorage inMemoryTraceStorage;
    private final TempoProperties tempoProperties;
    
    /**
     * Checks if the trace client is enabled.
     *
     * @return true if trace client is enabled, false otherwise
     */
    public boolean isTraceClientEnabled() {
        return traceClient.isEnabled();
    }
    
    /**
     * Retrieves and converts trace data for the given tryId.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>In Tempo mode: Check in-memory cache first (fast path)</li>
     *   <li>Queries for trace with the given tryId (slow path)</li>
     *   <li>Fetches trace data from storage</li>
     *   <li>Parses trace data from JSON</li>
     *   <li>Converts TraceDTO to TraceSpanInfo list</li>
     * </ol>
     *
     * @param tryIdStr Try session ID as a UUID string
     * @return Optional containing TraceDataResult with traceId and spans if found, empty otherwise
     */
    public Optional<TraceDataResult> getTraceData(String tryIdStr) {
        log.debug("Retrieving trace data for tryId: {}", tryIdStr);

        if (!traceClient.isEnabled()) {
            log.debug("Trace client is not enabled");
            return Optional.empty();
        }

        // In Tempo mode, check in-memory cache first (fast path)
        if (tempoProperties.isEnabled()) {
            TraceDTO cachedTrace = inMemoryTraceStorage.getTraceByTryId(tryIdStr);
            if (cachedTrace != null) {
                log.info("Trace found in memory cache (fast path): tryId={}", tryIdStr);
                try {
                    List<TraceSpanInfo> spans = traceSpanConverter.convert(cachedTrace);
                    String traceId = inMemoryTraceStorage.getTraceId(tryIdStr);
                    return Optional.of(new TraceDataResult(traceId, spans));
                } catch (Exception e) {
                    log.warn("Failed to convert cached trace, falling back to Tempo: tryId={}", tryIdStr, e);
                    // Fall through to Tempo polling
                }
            }
            log.debug("Trace not in cache, querying Tempo (slow path): tryId={}", tryIdStr);
        }

        try {
            // Query for trace with this tryId (slow path)
            String query = String.format("{ span.ouro.try_id = \"%s\" }", tryIdStr);
            String traceId = traceClient.pollForTrace(query);

            if (traceId == null) {
                log.debug("Trace not found for tryId: {}", tryIdStr);
                return Optional.empty();
            }

            // Fetch trace data
            String traceDataJson = traceClient.getTrace(traceId);

            if (traceDataJson == null) {
                log.warn("Trace data is null for traceId: {}", traceId);
                return Optional.empty();
            }

            // Parse trace data
            TraceDTO traceData = objectMapper.readValue(traceDataJson, TraceDTO.class);

            // Convert to TraceSpanInfo
            List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);

            // Note: Do NOT cache Tempo results back to memory
            // Tempo is already the permanent storage, no need to duplicate

            return Optional.of(new TraceDataResult(traceId, spans));

        } catch (Exception e) {
            log.error("Error retrieving trace data for tryId: {}", tryIdStr, e);
            return Optional.empty();
        }
    }
    
    /**
     * Result class containing trace ID and converted spans.
     */
    public static class TraceDataResult {
        private final String traceId;
        private final List<TraceSpanInfo> spans;
        
        public TraceDataResult(String traceId, List<TraceSpanInfo> spans) {
            this.traceId = traceId;
            this.spans = spans;
        }
        
        public String getTraceId() {
            return traceId;
        }
        
        public List<TraceSpanInfo> getSpans() {
            return spans;
        }
    }
}

