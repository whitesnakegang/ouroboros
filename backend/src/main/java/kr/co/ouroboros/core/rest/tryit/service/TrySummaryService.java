package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.ui.rest.tryit.dto.TrySummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for retrieving Try summary information without trace spans or issues.
 * <p>
 * This service is optimized for dashboard and list views, providing only
 * metadata, counts, and status information without detailed trace data or
 * issue analysis.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Retrieves summary metadata (traceId, status, duration, counts)</li>
 *   <li>Calculates total duration and span count</li>
 *   <li>Detects issues without full trace tree building</li>
 *   <li>Extracts HTTP status code from trace spans</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrySummaryService {
    
    private final TempoClient tempoClient;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final IssueAnalyzer issueAnalyzer;
    
    /**
     * Retrieves summary information for a Try without trace spans or issues.
     * <p>
     * Includes only metadata, counts, and status information:
     * <ul>
     *   <li>tryId and traceId</li>
     *   <li>Analysis status (PENDING, COMPLETED, FAILED)</li>
     *   <li>HTTP status code</li>
     *   <li>Total duration in milliseconds</li>
     *   <li>Span count and issue count</li>
     * </ul>
     *
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @return Summary response with metadata and counts
     * @throws Exception if retrieval fails
     */
    public TrySummaryResponse getSummary(String tryIdStr) {
        log.info("Retrieving summary for tryId: {}", tryIdStr);
        
        // Check if Tempo is enabled
        if (!tempoClient.isEnabled()) {
            log.debug("Tempo is not enabled, returning pending summary");
            return buildEmptySummary(tryIdStr);
        }
        
        try {
            // Query Tempo for trace with this tryId
            String query = String.format("{ span.ouro.try_id = \"%s\" }", tryIdStr);
            String traceId = tempoClient.pollForTrace(query);
            
            if (traceId == null) {
                log.debug("Trace not found in Tempo for tryId: {}", tryIdStr);
                return buildEmptySummary(tryIdStr);
            }
            
            // Fetch trace data
            String traceDataJson = tempoClient.getTrace(traceId);
            
            if (traceDataJson == null) {
                log.warn("Trace data is null for traceId: {}", traceId);
                return buildEmptySummary(tryIdStr);
            }
            
            // Parse trace data
            TraceDTO traceData = objectMapper.readValue(traceDataJson, TraceDTO.class);
            
            // Convert to TraceSpanInfo
            List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(spans);
            
            // Count issues (only issues, not full tree)
            var issues = issueAnalyzer.analyze(spans, totalDurationMs);
            int issueCount = issues != null ? issues.size() : 0;
            
            // Extract HTTP status code
            Integer statusCode = extractHttpStatusCode(spans);
            
            return TrySummaryResponse.builder()
                    .tryId(tryIdStr)
                    .traceId(traceId)
                    .status(AnalysisStatus.COMPLETED)
                    .statusCode(statusCode != null ? statusCode : 200)
                    .totalDurationMs(totalDurationMs)
                    .spanCount(spans.size())
                    .issueCount(issueCount)
                    .build();
            
        } catch (Exception e) {
            log.error("Error retrieving summary for tryId: {}", tryIdStr, e);
            return TrySummaryResponse.builder()
                    .tryId(tryIdStr)
                    .traceId(null)
                    .status(AnalysisStatus.FAILED)
                    .error("Failed to retrieve summary: " + e.getMessage())
                    .totalDurationMs(0L)
                    .spanCount(0)
                    .issueCount(0)
                    .build();
        }
    }
    
    /**
     * Builds an empty summary response when no trace data is available.
     * <p>
     * Returns a summary with PENDING status and zero counts.
     *
     * @param tryId Try session ID
     * @return Empty summary response with PENDING status
     */
    private TrySummaryResponse buildEmptySummary(String tryId) {
        return TrySummaryResponse.builder()
                .tryId(tryId)
                .traceId(null)
                .status(AnalysisStatus.PENDING)
                .totalDurationMs(0L)
                .spanCount(0)
                .issueCount(0)
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
    
    /**
     * Extracts HTTP status code from server span attributes.
     * <p>
     * Searches for spans with kind "SERVER" or names starting with "http",
     * then extracts the status code from attributes using common keys:
     * <ul>
     *   <li>http.status_code</li>
     *   <li>status</li>
     * </ul>
     *
     * @param spans List of trace span information
     * @return HTTP status code if found, null otherwise
     */
    private Integer extractHttpStatusCode(List<TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }
        
        for (TraceSpanInfo span : spans) {
            // Prefer server span or http-named span
            boolean maybeHttp = ("SERVER".equals(span.getKind()))
                    || (span.getName() != null && span.getName().toLowerCase().startsWith("http"));
            
            if (!maybeHttp || span.getAttributes() == null) {
                continue;
            }
            
            // Check common status code attribute keys
            String statusCodeStr = span.getAttributes().get("http.status_code");
            if (statusCodeStr == null) {
                statusCodeStr = span.getAttributes().get("status");
            }
            
            if (statusCodeStr != null) {
                try {
                    return Integer.parseInt(statusCodeStr);
                } catch (NumberFormatException ignored) {
                    // Continue to next span
                }
            }
        }
        
        return null;
    }
}

