package kr.co.ouroboros.core.rest.tryit.service;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceDataRetriever;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus;
import kr.co.ouroboros.core.rest.tryit.trace.util.TraceDurationCalculator;
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
    
    private final TraceDataRetriever traceDataRetriever;
    private final IssueAnalyzer issueAnalyzer;
    
    /**
     * Retrieve a compact summary for a Try session without returning trace spans or full issue details.
     *
     * @param tryIdStr Try session ID as a UUID string used to locate the corresponding trace.
     * @return a TrySummaryResponse containing tryId, traceId (if found), analysis status (PENDING, COMPLETED, or FAILED),
     *         HTTP status code, totalDurationMs, spanCount, issueCount, and an error message when retrieval fails.
     */
    public TrySummaryResponse getSummary(String tryIdStr) {
        log.info("Retrieving summary for tryId: {}", tryIdStr);
        
        return traceDataRetriever.getTraceData(tryIdStr)
                .map(result -> {
                    List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans = result.getSpans();
                    String traceId = result.getTraceId();
                    
                    // Calculate total duration
                    long totalDurationMs = TraceDurationCalculator.calculateTotalDuration(spans);
                    
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
                })
                .orElse(buildEmptySummary(tryIdStr));
    }
    
    /**
     * Build a TrySummaryResponse representing a pending Try with no trace data.
     *
     * @param tryId the Try session ID
     * @return a TrySummaryResponse with status PENDING, null traceId, and zeroed duration, span, and issue counts
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
    private Integer extractHttpStatusCode(List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }
        
        for (kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo span : spans) {
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
