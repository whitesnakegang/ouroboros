package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.util.SpanFlattener;
import kr.co.ouroboros.core.rest.tryit.web.dto.TryMethodListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving try method list with pagination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TryMethodListService {
    
    private final TempoClient tempoClient;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final TraceTreeBuilder traceTreeBuilder;
    private final SpanFlattener spanFlattener;
    
    /**
     * Retrieves paginated list of methods for a try, sorted by selfDurationMs (descending).
     * 
     * @param tryIdStr Try session ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated method list response
     */
    public TryMethodListResponse getMethodList(String tryIdStr, int page, int size) {
        log.info("Retrieving method list for tryId: {}, page: {}, size: {}", tryIdStr, page, size);
        
        // Check if Tempo is enabled
        if (!tempoClient.isEnabled()) {
            log.debug("Tempo is not enabled, returning empty response");
            return buildEmptyResponse(tryIdStr, page, size);
        }
        
        try {
            // Query Tempo for trace with this tryId
            String query = String.format("{ span.ouro.try_id = \"%s\" }", tryIdStr);
            String traceId = tempoClient.pollForTrace(query);
            
            if (traceId == null) {
                log.debug("Trace not found in Tempo for tryId: {}", tryIdStr);
                return buildEmptyResponse(tryIdStr, page, size);
            }
            
            // Fetch trace data
            String traceDataJson = tempoClient.getTrace(traceId);
            
            if (traceDataJson == null) {
                log.warn("Trace data is null for traceId: {}", traceId);
                return buildEmptyResponse(tryIdStr, page, size);
            }
            
            // Parse trace data
            TraceDTO traceData = objectMapper.readValue(traceDataJson, TraceDTO.class);
            
            // Convert to TraceSpanInfo
            List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(spans);
            
            // Build tree
            List<SpanNode> spanTree = traceTreeBuilder.buildTree(spans, totalDurationMs);
            
            // Flatten the tree
            List<SpanNode> flatSpans = spanFlattener.flatten(spanTree);
            
            // Sort by selfDurationMs descending
            List<SpanNode> sortedSpans = flatSpans.stream()
                    .sorted(Comparator
                            .comparing((SpanNode node) -> 
                                    node.getSelfDurationMs() != null ? node.getSelfDurationMs() : 0L)
                            .reversed())
                    .collect(Collectors.toList());
            
            // Apply pagination
            int totalCount = sortedSpans.size();
            int start = page * size;
            int end = Math.min(start + size, totalCount);
            boolean hasMore = end < totalCount;
            
            List<SpanNode> paginatedSpans = start < totalCount 
                    ? sortedSpans.subList(start, end)
                    : List.of();
            
            // Convert SpanNode to MethodInfo
            List<TryMethodListResponse.MethodInfo> methods = paginatedSpans.stream()
                    .map(this::convertToMethodInfo)
                    .collect(Collectors.toList());
            
            return TryMethodListResponse.builder()
                    .tryId(tryIdStr)
                    .traceId(traceId)
                    .totalDurationMs(totalDurationMs)
                    .totalCount(totalCount)
                    .page(page)
                    .size(size)
                    .hasMore(hasMore)
                    .methods(methods)
                    .build();
            
        } catch (Exception e) {
            log.error("Error retrieving method list for tryId: {}", tryIdStr, e);
            return buildEmptyResponse(tryIdStr, page, size);
        }
    }
    
    /**
     * Builds an empty response when no data is available.
     */
    private TryMethodListResponse buildEmptyResponse(String tryId, int page, int size) {
        return TryMethodListResponse.builder()
                .tryId(tryId)
                .traceId(null)
                .totalDurationMs(0L)
                .totalCount(0)
                .page(page)
                .size(size)
                .hasMore(false)
                .methods(List.of())
                .build();
    }
    
    /**
     * Calculates total duration of the trace.
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
     * Converts SpanNode to MethodInfo.
     */
    private TryMethodListResponse.MethodInfo convertToMethodInfo(SpanNode spanNode) {
        // Convert parameters
        List<TryMethodListResponse.MethodInfo.Parameter> parameters = null;
        if (spanNode.getParameters() != null) {
            parameters = spanNode.getParameters().stream()
                    .map(param -> TryMethodListResponse.MethodInfo.Parameter.builder()
                            .type(param.getType())
                            .name(param.getName())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return TryMethodListResponse.MethodInfo.builder()
                .spanId(spanNode.getSpanId())
                .className(spanNode.getClassName())
                .methodName(spanNode.getMethodName())
                .parameters(parameters)
                .selfDurationMs(spanNode.getSelfDurationMs())
                .selfPercentage(spanNode.getSelfPercentage())
                .build();
    }
}
