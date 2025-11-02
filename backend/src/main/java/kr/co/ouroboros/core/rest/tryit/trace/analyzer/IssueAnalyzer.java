package kr.co.ouroboros.core.rest.tryit.trace.analyzer;

import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes trace spans and detects issues (bottlenecks).
 */
@Slf4j
@Component
public class IssueAnalyzer {
    
    /**
     * Detects issues in the trace.
     * 
     * @param spans List of spans
     * @param totalDurationMs Total duration
     * @return List of detected issues
     */
    public List<Issue> analyze(List<TraceSpanInfo> spans, long totalDurationMs) {
        if (spans == null || spans.isEmpty()) {
            log.debug("Empty spans list, no issues detected");
            return new ArrayList<>();
        }
        
        List<Issue> issues = new ArrayList<>();
        
        for (TraceSpanInfo span : spans) {
            long durationMs = span.getDurationNanos() != null ? span.getDurationNanos() / 1_000_000 : 0;
            double percentage = totalDurationMs > 0 ? (durationMs * 100.0 / totalDurationMs) : 0;
            
            // Detect slow DB queries
            if (isDatabaseSpan(span) && percentage > 50 && durationMs > 500) {
                issues.add(Issue.builder()
                        .type(Issue.Type.DB_QUERY_SLOW)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("DB query takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.getName())
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Check index usage and query optimization")
                        .build());
            }
            
            // Detect slow HTTP calls
            if (isHttpSpan(span) && percentage > 30 && durationMs > 300) {
                issues.add(Issue.builder()
                        .type(Issue.Type.SLOW_HTTP)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("HTTP call takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.getName())
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Optimize external API calls or add caching")
                        .build());
            }
            
            // Detect generally slow spans
            if (percentage > 20 && durationMs > 100) {
                issues.add(Issue.builder()
                        .type(Issue.Type.SLOW_SPAN)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("Span takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.getName())
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Review method implementation for optimization")
                        .build());
            }
        }
        
        log.debug("Detected {} issues in trace", issues.size());
        return issues;
    }
    
    /**
     * Checks if span is a database operation.
     * 
     * @param span Span info
     * @return true if database span
     */
    private boolean isDatabaseSpan(TraceSpanInfo span) {
        String name = span.getName() != null ? span.getName().toLowerCase() : "";
        return name.contains("repository") ||
               name.contains("jdbc") ||
               name.contains("query") ||
               name.contains("execute") ||
               name.contains("db");
    }
    
    /**
     * Checks if span is an HTTP operation.
     * 
     * @param span Span info
     * @return true if HTTP span
     */
    private boolean isHttpSpan(TraceSpanInfo span) {
        String name = span.getName() != null ? span.getName() : "";
        return name.startsWith("HTTP") ||
               name.startsWith("http") ||
               name.toLowerCase().contains("http.method") ||
               name.toLowerCase().contains("http.status_code");
    }
    
    /**
     * Determines issue severity based on percentage.
     * 
     * @param percentage Percentage of total time
     * @return Severity level
     */
    private Issue.Severity determineSeverity(double percentage) {
        if (percentage >= 75) {
            return Issue.Severity.CRITICAL;
        } else if (percentage >= 50) {
            return Issue.Severity.HIGH;
        } else if (percentage >= 25) {
            return Issue.Severity.MEDIUM;
        } else {
            return Issue.Severity.LOW;
        }
    }
    
    /**
     * Builds evidence list for an issue.
     * 
     * @param span Span info
     * @return Evidence list
     */
    private List<String> buildEvidence(TraceSpanInfo span) {
        List<String> evidence = new ArrayList<>();
        evidence.add("duration: " + (span.getDurationNanos() != null ? span.getDurationNanos() / 1_000_000 + "ms" : "unknown"));
        evidence.add("kind: " + span.getKind());
        evidence.add("name: " + span.getName());
        
        return evidence;
    }
}

