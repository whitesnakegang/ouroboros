package kr.co.ouroboros.core.rest.tryit.analysis;

import kr.co.ouroboros.core.rest.tryit.web.dto.TryResultResponse;
import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes trace data from Tempo and builds span trees.
 * Detects bottlenecks and issues in the trace.
 */
@Slf4j
@Service
public class TraceAnalyzer {
    
    /**
     * Analyzes trace data and builds response with span tree.
     * 
     * @param traceData Trace data from Tempo
     * @param totalDurationMs Total duration of the request
     * @return Analysis response with span tree and issues
     */
    public TryResultResponse analyze(TraceDTO traceData, long totalDurationMs) {
        if (traceData == null || traceData.getBatches() == null || traceData.getBatches().isEmpty()) {
            return buildEmptyResponse(totalDurationMs);
        }
        
        // Extract all spans
        List<SpanInfo> spans = extractSpans(traceData);
        
        if (spans.isEmpty()) {
            return buildEmptyResponse(totalDurationMs);
        }
        
        // Build hierarchical span tree
        List<TryResultResponse.SpanNode> spanTree = buildSpanTree(spans, totalDurationMs);
        
        // Detect issues
        List<TryResultResponse.Issue> issues = detectIssues(spans, totalDurationMs);
        
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
    
    /**
     * Extracts all spans from trace data.
     * 
     * @param traceData Trace data
     * @return List of span information
     */
    private List<SpanInfo> extractSpans(TraceDTO traceData) {
        List<SpanInfo> spans = new ArrayList<>();
        
        for (TraceDTO.BatchDTO batch : traceData.getBatches()) {
            if (batch.getScopeSpans() == null) {
                continue;
            }
            
            for (TraceDTO.ScopeSpanDTO scopeSpan : batch.getScopeSpans()) {
                if (scopeSpan.getSpans() == null) {
                    continue;
                }
                
                for (TraceDTO.SpanDTO span : scopeSpan.getSpans()) {
                    SpanInfo info = new SpanInfo();
                    info.spanId = span.getSpanId();
                    info.parentSpanId = span.getParentSpanId();
                    info.name = span.getName();
                    info.kind = mapSpanKind(span.getKind());
                    info.startTimeNanos = span.getStartTimeUnixNano();
                    info.endTimeNanos = span.getEndTimeUnixNano();
                    info.durationNanos = span.getDurationNanos();
                    // Compute duration if not provided
                    if (info.durationNanos == null && info.startTimeNanos != null && info.endTimeNanos != null) {
                        info.durationNanos = Math.max(0L, info.endTimeNanos - info.startTimeNanos);
                    }
                    info.durationMs = info.durationNanos != null ? info.durationNanos / 1_000_000 : 0L;
                    // Extract attributes into a map for easier access
                    info.attributes = new HashMap<>();
                    if (span.getAttributes() != null) {
                        for (TraceDTO.AttributeDTO attr : span.getAttributes()) {
                            if (attr.getKey() != null && attr.getValue() != null) {
                                String v = attr.getValue().getStringValue();
                                if (v == null && attr.getValue().getIntValue() != null) {
                                    v = String.valueOf(attr.getValue().getIntValue());
                                }
                                if (v == null && attr.getValue().getDoubleValue() != null) {
                                    v = String.valueOf(attr.getValue().getDoubleValue());
                                }
                                if (v == null && attr.getValue().getBoolValue() != null) {
                                    v = String.valueOf(attr.getValue().getBoolValue());
                                }
                                if (v != null) {
                                    info.attributes.put(attr.getKey(), v);
                                }
                            }
                        }
                    }
                    
                    spans.add(info);
                }
            }
        }
        
        return spans;
    }
    
    /**
     * Maps span kind integer to string.
     * 
     * @param kind Kind integer
     * @return Kind string
     */
    private String mapSpanKind(String kind) {
        if (kind == null) {
            return "INTERNAL";
        }
        // Convert Tempo's span kind format to our internal format
        switch (kind) {
            case "SPAN_KIND_UNSPECIFIED": return "UNSPECIFIED";
            case "SPAN_KIND_INTERNAL": return "INTERNAL";
            case "SPAN_KIND_SERVER": return "SERVER";
            case "SPAN_KIND_CLIENT": return "CLIENT";
            case "SPAN_KIND_PRODUCER": return "PRODUCER";
            case "SPAN_KIND_CONSUMER": return "CONSUMER";
            default: return "INTERNAL";
        }
    }
    
    
    /**
     * Builds hierarchical span tree from flat span list.
     * 
     * @param spans List of spans
     * @param totalDurationMs Total duration
     * @return Root spans (with children)
     */
    private List<TryResultResponse.SpanNode> buildSpanTree(List<SpanInfo> spans, long totalDurationMs) {
        // Group spans by parentSpanId
        Map<String, List<SpanInfo>> childrenMap = new HashMap<>();
        Map<String, SpanInfo> spanMap = new HashMap<>();
        
        for (SpanInfo span : spans) {
            spanMap.put(span.spanId, span);
            
            String parentId = span.parentSpanId;
            if (parentId == null || parentId.isEmpty() || parentId.equals("0")) {
                parentId = "ROOT";
            }
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(span);
        }
        
        // Find root spans (spans with no parent or parent not in trace)
        List<SpanInfo> rootSpans = spans.stream()
                .filter(s -> s.parentSpanId == null || s.parentSpanId.isEmpty() || 
                           s.parentSpanId.equals("0") || !spanMap.containsKey(s.parentSpanId))
                .collect(Collectors.toList());
        
        // Build tree recursively
        return rootSpans.stream()
                .map(span -> buildSpanNode(span, spanMap, childrenMap, totalDurationMs))
                .collect(Collectors.toList());
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
    private TryResultResponse.SpanNode buildSpanNode(
            SpanInfo span,
            Map<String, SpanInfo> spanMap,
            Map<String, List<SpanInfo>> childrenMap,
            long totalDurationMs
    ) {
        long durationMs = span.durationMs != null ? span.durationMs : 0;
        double percentage = totalDurationMs > 0 ? (durationMs * 100.0 / totalDurationMs) : 0;
        
        // Parse class name and method signature (prefer attributes from interceptor)
        MethodInfo methodInfo = parseMethodInfo(span);
        
        // Get children
        List<TryResultResponse.SpanNode> children = new ArrayList<>();
        if (childrenMap.containsKey(span.spanId)) {
            children = childrenMap.get(span.spanId).stream()
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
        
        String displayName;
        if ("HTTP".equals(methodInfo.className)) {
            displayName = formatHttpDisplayName(span, methodInfo);
        } else if (methodInfo.className != null && !methodInfo.className.isEmpty()
                && methodInfo.methodName != null && !methodInfo.methodName.isEmpty()) {
            displayName = methodInfo.className + "." + methodInfo.methodName;
        } else {
            displayName = span.name;
        }

        return TryResultResponse.SpanNode.builder()
                .name(displayName)
                .className(methodInfo.className)
                .methodName(methodInfo.methodName)
                .parameters(methodInfo.parameters)
                .durationMs(durationMs)
                .selfDurationMs(selfDurationMs)
                .percentage(round2(percentage))
                .selfPercentage(round2(selfPercentage))
                .kind(span.kind)
                .children(children)
                .build();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Build human-friendly HTTP span display name using real URL if available.
     * Example: "http get /api/users/123" while methodName keeps template route.
     */
    private String formatHttpDisplayName(SpanInfo span, MethodInfo methodInfo) {
        String verb = null;
        if (span.attributes != null) {
            // Common keys used by our spans
            verb = span.attributes.get("method");
            if (verb == null) {
                verb = span.attributes.get("http.method");
            }
        }
        if (verb == null) {
            // fallback: try to get from original span name: "http get ..."
            if (span.name != null && span.name.startsWith("http ")) {
                String[] parts = span.name.split(" ", 3);
                if (parts.length >= 2) verb = parts[1];
            }
        }

        String path = null;
        if (span.attributes != null) {
            String httpUrl = span.attributes.get("http.url");
            if (httpUrl != null) {
                try {
                    java.net.URI u = java.net.URI.create(httpUrl);
                    path = u.getPath();
                    if (path == null || path.isEmpty()) path = httpUrl; // as-is fallback
                } catch (Exception ignored) {
                    path = httpUrl;
                }
            }
            if (path == null) {
                // Sometimes servers set "uri" with concrete value
                path = span.attributes.get("uri");
            }
        }

        if (verb != null && path != null) {
            return "http " + verb.toLowerCase() + " " + path;
        }
        // fallback to original
        return span.name != null ? span.name : (methodInfo.methodName != null ? methodInfo.methodName : "HTTP");
    }
    
    /**
     * Detects issues in the trace.
     * 
     * @param spans List of spans
     * @param totalDurationMs Total duration
     * @return List of detected issues
     */
    private List<TryResultResponse.Issue> detectIssues(List<SpanInfo> spans, long totalDurationMs) {
        List<TryResultResponse.Issue> issues = new ArrayList<>();
        
        for (SpanInfo span : spans) {
            long durationMs = span.durationNanos != null ? span.durationNanos / 1_000_000 : 0;
            double percentage = totalDurationMs > 0 ? (durationMs * 100.0 / totalDurationMs) : 0;
            
            // Detect slow DB queries
            if (isDatabaseSpan(span) && percentage > 50 && durationMs > 500) {
                issues.add(TryResultResponse.Issue.builder()
                        .type(TryResultResponse.Issue.Type.DB_QUERY_SLOW)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("DB query takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.name)
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Check index usage and query optimization")
                        .build());
            }
            
            // Detect slow HTTP calls
            if (isHttpSpan(span) && percentage > 30 && durationMs > 300) {
                issues.add(TryResultResponse.Issue.builder()
                        .type(TryResultResponse.Issue.Type.SLOW_HTTP)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("HTTP call takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.name)
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Optimize external API calls or add caching")
                        .build());
            }
            
            // Detect generally slow spans
            if (percentage > 20 && durationMs > 100) {
                issues.add(TryResultResponse.Issue.builder()
                        .type(TryResultResponse.Issue.Type.SLOW_SPAN)
                        .severity(determineSeverity(percentage))
                        .summary(String.format("Span takes %.1f%% of total time (%dms)", percentage, durationMs))
                        .spanName(span.name)
                        .durationMs(durationMs)
                        .evidence(buildEvidence(span))
                        .recommendation("Review method implementation for optimization")
                        .build());
            }
        }
        
        return issues;
    }
    
    /**
     * Checks if span is a database operation.
     * 
     * @param span Span info
     * @return true if database span
     */
    private boolean isDatabaseSpan(SpanInfo span) {
        return span.name.toLowerCase().contains("repository") ||
               span.name.toLowerCase().contains("jdbc") ||
               span.name.toLowerCase().contains("query") ||
               span.name.toLowerCase().contains("execute") ||
               span.name.toLowerCase().contains("db");
    }
    
    /**
     * Checks if span is an HTTP operation.
     * 
     * @param span Span info
     * @return true if HTTP span
     */
    private boolean isHttpSpan(SpanInfo span) {
        return span.name.startsWith("HTTP") ||
               span.name.startsWith("http") ||
               span.name.toLowerCase().contains("http.method") ||
               span.name.toLowerCase().contains("http.status_code");
    }
    
    /**
     * Determines issue severity based on percentage.
     * 
     * @param percentage Percentage of total time
     * @return Severity level
     */
    private TryResultResponse.Issue.Severity determineSeverity(double percentage) {
        if (percentage >= 75) {
            return TryResultResponse.Issue.Severity.CRITICAL;
        } else if (percentage >= 50) {
            return TryResultResponse.Issue.Severity.HIGH;
        } else if (percentage >= 25) {
            return TryResultResponse.Issue.Severity.MEDIUM;
        } else {
            return TryResultResponse.Issue.Severity.LOW;
        }
    }
    
    /**
     * Builds evidence list for an issue.
     * 
     * @param span Span info
     * @return Evidence list
     */
    private List<String> buildEvidence(SpanInfo span) {
        List<String> evidence = new ArrayList<>();
        evidence.add("duration: " + (span.durationNanos != null ? span.durationNanos / 1_000_000 + "ms" : "unknown"));
        evidence.add("kind: " + span.kind);
        evidence.add("name: " + span.name);
        
        return evidence;
    }
    
    /**
     * Extracts class name and method signature from span name.
     * 
     * @param spanName Full span name (e.g., "OrderController.getOrder")
     * @return Parsed info containing className, methodName, and parameters
     */
    private MethodInfo parseMethodSignature(String spanName) {
        MethodInfo info = new MethodInfo();
        
        if (spanName == null || spanName.isEmpty()) {
            return info;
        }
        
        // Handle HTTP span names (e.g., "http get /api/users/{id}")
        if (spanName.startsWith("http ")) {
            info.className = "HTTP";
            info.methodName = spanName;
            return info;
        }
        
        // Try to parse class.method() pattern
        // Examples: "OrderController.getOrder", "OrderService.findById(Long)"
        int lastDotIndex = spanName.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < spanName.length() - 1) {
            // Extract class name
            info.className = spanName.substring(0, lastDotIndex);
            
            // Extract method part
            String methodPart = spanName.substring(lastDotIndex + 1);
            
            // Check if it contains parentheses (has parameters)
            int openParen = methodPart.indexOf('(');
            
            if (openParen > 0) {
                // Has parameters
                info.methodName = methodPart.substring(0, openParen);
                String paramsStr = methodPart.substring(openParen + 1, methodPart.lastIndexOf(')'));
                
                // Parse parameters
                if (!paramsStr.isEmpty()) {
                    info.parameters = java.util.Arrays.stream(paramsStr.split(","))
                            .map(String::trim)
                            .map(this::parseParameter)
                            .collect(java.util.stream.Collectors.toList());
                }
            } else {
                // No parameters
                info.methodName = methodPart;
            }
        } else {
            // Can't parse, treat entire name as method name
            info.methodName = spanName;
        }
        
        return info;
    }

    /**
     * Parses method info preferring OpenTelemetry attributes set by our interceptor.
     */
    private MethodInfo parseMethodInfo(SpanInfo span) {
        MethodInfo info = new MethodInfo();
        if (span == null) return info;
        // Attributes preferred
        String namespace = span.attributes != null ? span.attributes.get("code.namespace") : null;
        String function = span.attributes != null ? span.attributes.get("code.function") : null;
        if (namespace != null || function != null) {
            if (namespace != null) {
                int lastDot = namespace.lastIndexOf('.');
                info.className = lastDot >= 0 ? namespace.substring(lastDot + 1) : namespace;
            }
            if (function != null) {
                info.methodName = function;
            }
            // Parameters
            int idx = 0;
            while (true) {
                String type = span.attributes.get("code.parameter." + idx + ".type");
                String name = span.attributes.get("code.parameter." + idx + ".name");
                if (type == null && name == null) break;
                info.parameters.add(TryResultResponse.SpanNode.Parameter.builder()
                        .type(type != null ? type : "")
                        .name(name != null ? name : "")
                        .build());
                idx++;
            }
            return info;
        }
        // Fallback to span name parsing
        return parseMethodSignature(span.name);
    }
    
    /**
     * Parses a parameter string into Parameter object.
     * Supports formats:
     * - "Type" (only type, no name)
     * - "Type name" (type and name with space)
     * - "Type name" (type and name, will be parsed as "Type" and "name")
     * 
     * @param paramStr Parameter string
     * @return Parameter object
     */
    private TryResultResponse.SpanNode.Parameter parseParameter(String paramStr) {
        if (paramStr == null || paramStr.isEmpty()) {
            return TryResultResponse.SpanNode.Parameter.builder()
                    .type("")
                    .name("")
                    .build();
        }
        
        // Try to parse as "Type name" or just "Type"
        String trimmed = paramStr.trim();
        int spaceIndex = trimmed.lastIndexOf(' ');
        
        if (spaceIndex > 0 && spaceIndex < trimmed.length() - 1) {
            // Has both type and name
            String type = trimmed.substring(0, spaceIndex).trim();
            String name = trimmed.substring(spaceIndex + 1).trim();
            return TryResultResponse.SpanNode.Parameter.builder()
                    .type(type)
                    .name(name)
                    .build();
        } else {
            // Only type, no name (like "Long" or "String")
            return TryResultResponse.SpanNode.Parameter.builder()
                    .type(trimmed)
                    .name("")  // No name available
                    .build();
        }
    }
    
    /**
     * Internal class for method information.
     */
    private static class MethodInfo {
        String className;
        String methodName;
        List<TryResultResponse.SpanNode.Parameter> parameters;
        
        MethodInfo() {
            this.parameters = new ArrayList<>();
        }
    }
    
    /**
     * Internal class for span information.
     */
    private static class SpanInfo {
        String spanId;
        String parentSpanId;
        String name;
        String kind;
        Long startTimeNanos;
        Long endTimeNanos;
        Long durationNanos;
        Long durationMs;
        Map<String, String> attributes;
    }
}

