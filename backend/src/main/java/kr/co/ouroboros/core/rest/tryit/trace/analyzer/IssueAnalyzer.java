package kr.co.ouroboros.core.rest.tryit.trace.analyzer;

import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes trace spans and detects performance issues (bottlenecks).
 * <p>
 * This component analyzes trace spans to detect various performance issues
 * including slow database queries, slow HTTP calls, and general slow spans.
 * <p>
 * <b>Detected Issues:</b>
 * <ul>
 *   <li>Slow database queries (>50% of total time and >500ms)</li>
 *   <li>Slow HTTP calls (>30% of total time and >300ms)</li>
 *   <li>Slow spans (>20% of total time and >100ms)</li>
 * </ul>
 * <p>
 * Each detected issue includes type, severity, evidence, and recommendations.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class IssueAnalyzer {
    
    /**
     * Detects performance issues in the trace.
     * <p>
     * Analyzes each span in the trace and detects issues based on:
     * <ul>
     *   <li>Duration percentage of total time</li>
     *   <li>Absolute duration in milliseconds</li>
     *   <li>Span type (database, HTTP, general)</li>
     * </ul>
     * <p>
     * Returns a list of detected issues with severity, evidence, and recommendations.
     *
     * @param spans List of trace spans to analyze
     * @param totalDurationMs Total duration of the trace in milliseconds
     * @return List of detected performance issues
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
     * Checks if span represents a database operation.
     * <p>
     * Identifies database spans by checking span name for common database
     * keywords: repository, jdbc, query, execute, db.
     *
     * @param span Span information to check
     * @return true if span represents a database operation, false otherwise
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
     * Checks if span represents an HTTP operation.
     * <p>
     * Identifies HTTP spans by checking span name for HTTP-related patterns:
     * starts with "HTTP" or "http", or contains "http.method" or "http.status_code".
     *
     * @param span Span information to check
     * @return true if span represents an HTTP operation, false otherwise
     */
    private boolean isHttpSpan(TraceSpanInfo span) {
        String name = span.getName() != null ? span.getName() : "";
        return name.startsWith("HTTP") ||
               name.startsWith("http") ||
               name.toLowerCase().contains("http.method") ||
               name.toLowerCase().contains("http.status_code");
    }
    
    /**
     * Determines issue severity based on percentage of total trace time.
     * <p>
     * Severity levels:
     * <ul>
     *   <li>CRITICAL: ≥75% of total time</li>
     *   <li>HIGH: ≥50% and <75%</li>
     *   <li>MEDIUM: ≥25% and <50%</li>
     *   <li>LOW: <25%</li>
     * </ul>
     *
     * @param percentage Percentage of total trace time (0-100)
     * @return Severity level based on percentage threshold
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
     * Builds evidence list for a detected issue.
     * <p>
     * Creates a list of evidence strings including duration, kind, and name
     * to support the issue detection.
     *
     * @param span Span information to extract evidence from
     * @return List of evidence strings supporting the issue detection
     */
    private List<String> buildEvidence(TraceSpanInfo span) {
        List<String> evidence = new ArrayList<>();
        evidence.add("duration: " + (span.getDurationNanos() != null ? span.getDurationNanos() / 1_000_000 + "ms" : "unknown"));
        evidence.add("kind: " + span.getKind());
        evidence.add("name: " + span.getName());
        
        return evidence;
    }
}

