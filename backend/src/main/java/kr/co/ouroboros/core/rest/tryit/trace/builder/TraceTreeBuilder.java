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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceTreeBuilder {
    
    private final SpanMethodParser spanMethodParser;
    
    /**
     * Builds hierarchical span tree from flat span list.
     * 
     * @param spans List of spans
     * @param totalDurationMs Total duration
     * @return Root spans (with children)
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
     * Recursively builds a span node with its children.
     * 
     * @param span Span info
     * @param spanMap Map of span ID to span info
     * @param childrenMap Map of parent ID to children
     * @param totalDurationMs Total duration
     * @return Span node
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
     */
    private String buildDisplayName(TraceSpanInfo span, SpanMethodInfo methodInfo) {
        if ("HTTP".equals(methodInfo.getClassName())) {
            return formatHttpDisplayName(span, methodInfo);
        } else if (methodInfo.getClassName() != null && !methodInfo.getClassName().isEmpty()
                && methodInfo.getMethodName() != null && !methodInfo.getMethodName().isEmpty()) {
            return methodInfo.getClassName() + "." + methodInfo.getMethodName();
        } else {
            return span.getName();
        }
    }
    
    /**
     * Builds human-friendly HTTP span display name using real URL if available.
     * Example: "http get /api/users/123" while methodName keeps template route.
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
    
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

