package kr.co.ouroboros.core.rest.tryit.trace.builder;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanMethodInfo;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.trace.parser.SpanMethodParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds hierarchical span tree from TraceSpanInfo list.
 * <p>
 * This component builds a hierarchical tree structure from flat span lists,
 * organizing spans by parent-child relationships for trace visualization.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Builds parent-child relationships between spans</li>
 *   <li>Calculates total and self duration for each span</li>
 *   <li>Calculates percentage of total trace time</li>
 *   <li>Parses method information from span names</li>
 *   <li>Formats display names for HTTP spans</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceTreeBuilder {
    
    /**
     * Parser for extracting method information from span names.
     */
    private final SpanMethodParser spanMethodParser;
    
    /**
     * Builds a hierarchical tree of SpanNode objects from a flat list of TraceSpanInfo spans.
     *
     * Root spans are those with no parent, an empty or "0" parent ID, or whose parent is not present
     * in the provided list. Child relationships, durations, self-durations, and percentages are
     * computed for each node; method information and display names are derived for presentation.
     *
     * @param spans flat list of trace spans (may be null or empty)
     * @param totalDurationMs total duration of the entire trace in milliseconds used to compute percentages
     * @return list of root SpanNode objects, each containing its nested child nodes
     */
    public List<SpanNode> buildTree(List<TraceSpanInfo> spans, long totalDurationMs) {
        if (spans == null || spans.isEmpty()) {
            log.debug("Empty spans list, returning empty tree");
            return new ArrayList<>();
        }
        
        // Group spans by parentSpanId
        Map<String, List<TraceSpanInfo>> childrenMap = new HashMap<>();
        Map<String, TraceSpanInfo> spanMap = new HashMap<>();
        
        for (TraceSpanInfo span : spans) {
            spanMap.put(span.getSpanId(), span);
            
            String parentId = span.getParentSpanId();
            if (parentId == null || parentId.isEmpty() || parentId.equals("0")) {
                parentId = "ROOT";
            }
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(span);
        }
        
        // Find root spans (spans with no parent or parent not in trace)
        List<TraceSpanInfo> rootSpans = spans.stream()
                .filter(s -> s.getParentSpanId() == null || s.getParentSpanId().isEmpty() || 
                           s.getParentSpanId().equals("0") || !spanMap.containsKey(s.getParentSpanId()))
                .collect(Collectors.toList());
        
        // Build tree recursively
        List<SpanNode> tree = rootSpans.stream()
                .map(span -> buildSpanNode(span, spanMap, childrenMap, totalDurationMs))
                .collect(Collectors.toList());
        
        log.debug("Built tree with {} root spans", tree.size());
        return tree;
    }
    
    /**
     * Recursively constructs a SpanNode for the given span, including its nested children, computed durations, percentages, parsed method info, and formatted display name.
     *
     * <p>Self duration is calculated as (span duration - sum of children durations); this is an approximation and may overestimate self time if children execute in parallel.</p>
     *
     * @param span the TraceSpanInfo to convert into a SpanNode
     * @param spanMap map of span ID to TraceSpanInfo for lookup
     * @param childrenMap map of parent span ID to its child TraceSpanInfo list
     * @param totalDurationMs total duration of the trace in milliseconds (used to compute percentages)
     * @return the constructed SpanNode with children and computed metrics
     */
    private SpanNode buildSpanNode(
            TraceSpanInfo span,
            Map<String, TraceSpanInfo> spanMap,
            Map<String, List<TraceSpanInfo>> childrenMap,
            long totalDurationMs
    ) {
        long durationMs = span.getDurationMs() != null ? span.getDurationMs() : 0;
        double percentage = totalDurationMs > 0 ? (durationMs * 100.0 / totalDurationMs) : 0;
        
        // Parse class name and method signature
        SpanMethodInfo methodInfo = spanMethodParser.parse(span);
        
        // Get children
        List<SpanNode> children = new ArrayList<>();
        if (childrenMap.containsKey(span.getSpanId())) {
            children = childrenMap.get(span.getSpanId()).stream()
                    .map(child -> buildSpanNode(child, spanMap, childrenMap, totalDurationMs))
                    .collect(Collectors.toList());
        }
        
        // Calculate self duration (total duration - sum of children durations)
        // Note: This is an approximation. If children execute in parallel,
        // the actual self duration might be less than (total - sum of children).
        long childrenDuration = children.stream()
                .mapToLong(child -> child.getDurationMs() != null ? child.getDurationMs() : 0)
                .sum();
        long selfDurationMs = Math.max(0, durationMs - childrenDuration);
        
        // Calculate self percentage
        double selfPercentage = totalDurationMs > 0 ? (selfDurationMs * 100.0 / totalDurationMs) : 0;
        
        // Build display name
        String displayName = buildDisplayName(span, methodInfo);
        
        // Convert parameters
        List<SpanNode.Parameter> parameters = null;
        if (methodInfo.getParameters() != null) {
            parameters = methodInfo.getParameters().stream()
                    .map(param -> SpanNode.Parameter.builder()
                            .type(param.getType())
                            .name(param.getName())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return SpanNode.builder()
                .spanId(span.getSpanId())
                .name(displayName)
                .className(methodInfo.getClassName())
                .methodName(methodInfo.getMethodName())
                .parameters(parameters)
                .durationMs(durationMs)
                .selfDurationMs(selfDurationMs)
                .percentage(round2(percentage))
                .selfPercentage(round2(selfPercentage))
                .kind(span.getKind())
                .children(children)
                .build();
    }
    
    /**
     * Builds display name for the span.
     * <p>
     * Constructs a human-readable display name based on span type:
     * <ul>
     *   <li>HTTP spans: Uses formatted HTTP display name (e.g., "http get /api/users")</li>
     *   <li>Method spans: Uses class.method format (e.g., "OrderController.getOrder")</li>
     *   <li>Others: Uses original span name</li>
     * </ul>
     *
     * @param span Span information
     * @param methodInfo Parsed method information
     * @return Display name for the span
     */
    private String buildDisplayName(TraceSpanInfo span, SpanMethodInfo methodInfo) {
        if ("HTTP".equals(methodInfo.getClassName())) {
            return formatHttpDisplayName(span, methodInfo);
        } else if (methodInfo.getClassName() != null && !methodInfo.getClassName().isEmpty()
                && methodInfo.getMethodName() != null && !methodInfo.getMethodName().isEmpty()) {
            // Extract simple class name (without package path) for display name
            String simpleClassName = extractSimpleClassName(methodInfo.getClassName());
            return simpleClassName + "." + methodInfo.getMethodName();
        } else {
            return span.getName();
        }
    }
    
    /**
     * Returns the simple class name extracted from a fully qualified class name.
     *
     * @param fqcn the fully qualified class name (may be null or empty), e.g. "kr.co.ouroboros.service.DataProcessingService"
     * @return the simple class name (e.g. "DataProcessingService"), or the original {@code fqcn} if it is null or empty
     */
    private String extractSimpleClassName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) {
            return fqcn;
        }
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }
    
    /**
     * Builds human-friendly HTTP span display name using real URL if available.
     * <p>
     * Formats HTTP spans as "http {method} {path}" using actual HTTP method and URL path
     * from span attributes.
     * <p>
     * Example: "http get /api/users/123"
     *
     * @param span Span information containing HTTP attributes
     * @param methodInfo Parsed method information (fallback)
     * @return Formatted HTTP display name
     */
    private String formatHttpDisplayName(TraceSpanInfo span, SpanMethodInfo methodInfo) {
        String verb = null;
        if (span.getAttributes() != null) {
            // Common keys used by our spans
            verb = span.getAttributes().get("method");
            if (verb == null) {
                verb = span.getAttributes().get("http.method");
            }
        }
        if (verb == null) {
            // fallback: try to get from original span name: "http get ..."
            if (span.getName() != null && span.getName().startsWith("http ")) {
                String[] parts = span.getName().split(" ", 3);
                if (parts.length >= 2) verb = parts[1];
            }
        }
        
        String path = null;
        if (span.getAttributes() != null) {
            String httpUrl = span.getAttributes().get("http.url");
            if (httpUrl != null) {
                try {
                    URI uri = URI.create(httpUrl);
                    path = uri.getPath();
                    if (path == null || path.isEmpty()) path = httpUrl; // as-is fallback
                } catch (Exception ignored) {
                    path = httpUrl;
                }
            }
            if (path == null) {
                // Sometimes servers set "uri" with concrete value
                path = span.getAttributes().get("uri");
            }
        }
        
        if (verb != null && path != null) {
            return "http " + verb.toLowerCase() + " " + path;
        }
        // fallback to original
        return span.getName() != null ? span.getName() : 
               (methodInfo.getMethodName() != null ? methodInfo.getMethodName() : "HTTP");
    }
    
    /**
     * Round a numeric value to two decimal places.
     *
     * @param value the number to round
     * @return the input rounded to two decimal places
     */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}