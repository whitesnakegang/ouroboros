package kr.co.ouroboros.core.rest.tryit.service;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceDataRetriever;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.util.TraceDurationCalculator;
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
    
    private final TraceDataRetriever traceDataRetriever;
    private final IssueAnalyzer issueAnalyzer;
    
    /**
     * Retrieve detected performance issues for a Try session without constructing a trace tree.
     *
     * @param tryIdStr Try session ID as a UUID string used to locate trace data
     * @return a TryIssuesResponse containing the given tryId and the list of detected issues (empty list when no trace or on failure)
     */
    public TryIssuesResponse getIssues(String tryIdStr) {
        log.info("Retrieving issues for tryId: {}", tryIdStr);
        
        return traceDataRetriever.getTraceData(tryIdStr)
                .map(result -> {
                    List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans = result.getSpans();
                    
                    // Calculate total duration
                    long totalDurationMs = TraceDurationCalculator.calculateTotalDuration(spans);
                    
                    // Detect issues (without building tree for performance)
                    var issues = issueAnalyzer.analyze(spans, totalDurationMs);
                    
                    return TryIssuesResponse.builder()
                            .tryId(tryIdStr)
                            .issues(issues)
                            .build();
                })
                .orElse(buildEmptyResponse(tryIdStr));
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
}
