package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.ui.rest.tryit.dto.TryIssuesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for retrieving Try issues without trace spans.
 * <p>
 * This service is optimized for issues analysis and recommendations,
 * providing detected performance issues without full trace tree structure
 * for better performance.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Detects performance bottlenecks</li>
 *   <li>Identifies N+1 query problems</li>
 *   <li>Detects slow HTTP calls and database queries</li>
 *   <li>Provides recommendations for fixing issues</li>
 *   <li>Skips tree building for performance</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TryIssuesService {
    
    private final TempoClient tempoClient;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final IssueAnalyzer issueAnalyzer;
    
    /**
     * Retrieve detected performance issues for a Try session without constructing a trace tree.
     *
     * @param tryIdStr Try session ID as a UUID string used to locate trace data
     * @return a TryIssuesResponse containing the given tryId and the list of detected issues (empty list when no trace or on failure)
     */
    public TryIssuesResponse getIssues(String tryIdStr) {
        log.info("Retrieving issues for tryId: {}", tryIdStr);
        
        // Check if Tempo is enabled
        if (!tempoClient.isEnabled()) {
            log.debug("Tempo is not enabled, returning empty issues");
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
            var spans = traceSpanConverter.convert(traceData);
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(spans);
            
            // Detect issues (without building tree for performance)
            var issues = issueAnalyzer.analyze(spans, totalDurationMs);
            
            return TryIssuesResponse.builder()
                    .tryId(tryIdStr)
                    .issues(issues)
                    .build();
            
        } catch (Exception e) {
            log.error("Error retrieving issues for tryId: {}", tryIdStr, e);
            return buildEmptyResponse(tryIdStr);
        }
    }
    
    /**
     * Builds an empty issues response when no issues are detected or trace data is unavailable.
     *
     * @param tryId Try session ID
     * @return Empty issues response with empty issue list
     */
    private TryIssuesResponse buildEmptyResponse(String tryId) {
        return TryIssuesResponse.builder()
                .tryId(tryId)
                .issues(List.of())
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
    private long calculateTotalDuration(List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return 0;
        }
        
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        
        for (var span : spans) {
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
