package kr.co.ouroboros.core.rest.tryit.service;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceDataRetriever;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.util.TraceDurationCalculator;
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
    
    private final TraceDataRetriever traceDataRetriever;
    private final TraceTreeBuilder traceTreeBuilder;
    private final TraceStorage traceStorage;
    
    /**
     * Retrieve the full call trace for a Try without performing issue analysis.
     *
     * <p>If Tempo is disabled or no trace data is found, an empty TryTraceResponse is returned.
     *
     * @param tryIdStr Try session ID; expected to be a UUID string
     * @return TryTraceResponse containing the tryId, the found traceId (or null), totalDurationMs, and a hierarchical span tree; an empty response is returned when no trace is available
     */
    public TryTraceResponse getTrace(String tryIdStr) {
        log.info("Retrieving trace for tryId: {}", tryIdStr);
        
        return traceDataRetriever.getTraceData(tryIdStr)
                .map(result -> {
                    List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans = result.getSpans();
                    String traceId = result.getTraceId();
                    
                    // Calculate total duration
                    long totalDurationMs = TraceDurationCalculator.calculateTotalDuration(spans);
                    
                    // Build tree (without issues analysis for performance)
                    List<SpanNode> spanTree = traceTreeBuilder.buildTree(spans, totalDurationMs);
                    
                    return TryTraceResponse.builder()
                            .tryId(tryIdStr)
                            .traceId(traceId)
                            .totalDurationMs(totalDurationMs)
                            .spans(spanTree)
                            .build();
                })
                .orElse(buildEmptyResponse(tryIdStr));
    }
    
    /**
     * Deletes trace data for the given tryId from trace storage.
     * <p>
     * This method removes the trace data stored in the trace storage (e.g., in-memory storage)
     * for the specified tryId.
     *
     * @param tryIdStr Try session ID as a UUID string
     * @return true if trace was found and deleted, false otherwise
     */
    public boolean deleteTrace(String tryIdStr) {
        log.info("Deleting trace for tryId: {}", tryIdStr);
        boolean deleted = traceStorage.deleteTraceByTryId(tryIdStr);
        if (deleted) {
            log.info("Successfully deleted trace for tryId: {}", tryIdStr);
        } else {
            log.debug("Trace not found for deletion: tryId={}", tryIdStr);
        }
        return deleted;
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
}
