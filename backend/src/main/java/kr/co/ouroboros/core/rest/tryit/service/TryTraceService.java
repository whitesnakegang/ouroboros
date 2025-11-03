package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.ui.rest.tryit.dto.TryTraceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for retrieving Try trace information without analysis issues.
 * <p>
 * This service is optimized for call trace visualization (toggle tree view),
 * providing hierarchical span structure without issue analysis for better
 * performance.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Retrieves full call trace with hierarchical spans</li>
 *   <li>Builds trace tree structure</li>
 *   <li>Calculates total duration</li>
 *   <li>Skips issue detection for performance</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TryTraceService {
    
    private final TempoClient tempoClient;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final TraceTreeBuilder traceTreeBuilder;
    
    /**
     * Retrieves full call trace for a Try without issue analysis.
     * <p>
     * This method is optimized for performance by skipping issue detection:
     * <ol>
     *   <li>Retrieves trace data from Tempo using tryId</li>
     *   <li>Builds hierarchical trace tree</li>
     *   <li>Calculates total duration</li>
     *   <li>Returns tree structure with spans</li>
     * </ol>
     *
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @return Trace response with hierarchical spans
     * @throws Exception if retrieval fails
     */
    public TryTraceResponse getTrace(String tryIdStr) {
        log.info("Retrieving trace for tryId: {}", tryIdStr);
        
        // Check if Tempo is enabled
        if (!tempoClient.isEnabled()) {
            log.debug("Tempo is not enabled, returning empty trace");
            return buildEmptyResponse(tryIdStr);
        }
        
        try {
            // Query Tempo for trace with this tryId
            String query = String.format("{ span.ouro.try_id = \"%s\" }", tryIdStr);
            String traceId = tempoClient.pollForTrace(query);
            
            if (traceId == null) {
                log.debug("Trace not found in Tempo for tryId: {}", tryIdStr);
                return buildEmptyResponse(tryIdStr);
            }
            
            // Fetch trace data
            String traceDataJson = tempoClient.getTrace(traceId);
            
            if (traceDataJson == null) {
                log.warn("Trace data is null for traceId: {}", traceId);
                return buildEmptyResponse(tryIdStr);
            }
            
            // Parse trace data
            TraceDTO traceData = objectMapper.readValue(traceDataJson, TraceDTO.class);
            
            // Convert to TraceSpanInfo
            List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(spans);
            
            // Build tree (without issues analysis for performance)
            List<SpanNode> spanTree = traceTreeBuilder.buildTree(spans, totalDurationMs);
            
            return TryTraceResponse.builder()
                    .tryId(tryIdStr)
                    .traceId(traceId)
                    .totalDurationMs(totalDurationMs)
                    .spans(spanTree)
                    .build();
            
        } catch (Exception e) {
            log.error("Error retrieving trace for tryId: {}", tryIdStr, e);
            return buildEmptyResponse(tryIdStr);
        }
    }
    
    /**
     * Builds an empty trace response when no trace data is available.
     *
     * @param tryId Try session ID
     * @return Empty trace response with zero duration and empty spans
     */
    private TryTraceResponse buildEmptyResponse(String tryId) {
        return TryTraceResponse.builder()
                .tryId(tryId)
                .traceId(null)
                .totalDurationMs(0L)
                .spans(List.of())
                .build();
    }
    
    /**
     * Calculates total duration of the trace from span timestamps.
     * <p>
     * Finds the earliest start time and latest end time across all spans,
     * then calculates the difference in milliseconds.
     *
     * @param spans List of trace span information
     * @return Total duration in milliseconds, or 0 if spans are empty or invalid
     */
    private long calculateTotalDuration(List<TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return 0;
        }
        
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        
        for (TraceSpanInfo span : spans) {
            if (span.getStartTimeNanos() != null && span.getEndTimeNanos() != null) {
                minStart = Math.min(minStart, span.getStartTimeNanos());
                maxEnd = Math.max(maxEnd, span.getEndTimeNanos());
            }
        }
        
        if (minStart == Long.MAX_VALUE || maxEnd == Long.MIN_VALUE) {
            return 0;
        }
        
        return (maxEnd - minStart) / 1_000_000; // Convert nanoseconds to milliseconds
    }
}

