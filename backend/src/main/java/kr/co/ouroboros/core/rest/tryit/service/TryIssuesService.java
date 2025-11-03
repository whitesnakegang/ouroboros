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
 * Service for retrieving try issues without trace spans.
 * Optimized for issues analysis and recommendations.
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
     * Retrieves detected issues for a try without trace spans.
     * This is optimized for performance by skipping tree building.
     * 
     * @param tryIdStr Try session ID
     * @return Issues response
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
     * Builds an empty response when no issues data is available.
     */
    private TryIssuesResponse buildEmptyResponse(String tryId) {
        return TryIssuesResponse.builder()
                .tryId(tryId)
                .issues(List.of())
                .build();
    }
    
    /**
     * Calculates total duration of the trace.
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

