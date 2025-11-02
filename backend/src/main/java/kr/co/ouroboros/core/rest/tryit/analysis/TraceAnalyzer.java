package kr.co.ouroboros.core.rest.tryit.analysis;

import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.web.dto.TryResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates trace analysis and builds response with span tree and issues.
 * This is a facade that orchestrates the modular trace processing components.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceAnalyzer {
    
    private final TraceSpanConverter traceSpanConverter;
    private final TraceTreeBuilder traceTreeBuilder;
    private final IssueAnalyzer issueAnalyzer;
    
    /**
     * Analyzes trace data and builds response with span tree and issues.
     * 
     * @param traceData Trace data from Tempo
     * @param totalDurationMs Total duration of the request
     * @return Analysis response with span tree and issues
     */
    public TryResultResponse analyze(TraceDTO traceData, long totalDurationMs) {
        if (traceData == null || traceData.getBatches() == null || traceData.getBatches().isEmpty()) {
            log.debug("TraceData is null or empty, returning pending response");
            return buildEmptyResponse(totalDurationMs);
        }
        
        // Convert TraceDTO to TraceSpanInfo
        List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);
        
        if (spans.isEmpty()) {
            log.debug("No spans found in trace data");
            return buildEmptyResponse(totalDurationMs);
        }
        
        // Build hierarchical span tree
        List<TryResultResponse.SpanNode> spanTree = traceTreeBuilder.buildTree(spans, totalDurationMs);
        
        // Detect issues
        List<TryResultResponse.Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);
        
        log.debug("Analysis complete: {} spans, {} root nodes, {} issues", 
                spans.size(), spanTree.size(), issues.size());
        
        return TryResultResponse.builder()
                .status(TryResultResponse.Status.COMPLETED)
                .totalDurationMs(totalDurationMs)
                .spans(spanTree)
                .issues(issues)
                .spanCount(spans.size())
                .build();
    }
    
    /**
     * Builds an empty response when no trace data is available.
     * 
     * @param totalDurationMs Total duration
     * @return Empty response
     */
    private TryResultResponse buildEmptyResponse(long totalDurationMs) {
        return TryResultResponse.builder()
                .status(TryResultResponse.Status.PENDING)
                .totalDurationMs(totalDurationMs)
                .spans(new ArrayList<>())
                .issues(new ArrayList<>())
                .spanCount(0)
                .build();
    }
}
