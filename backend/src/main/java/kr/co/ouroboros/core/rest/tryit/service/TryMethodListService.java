package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.trace.util.SpanFlattener;
import kr.co.ouroboros.ui.rest.tryit.dto.TryMethodListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving Try method list with pagination.
 * <p>
 * This service provides paginated method lists sorted by self-duration
 * (descending), optimized for method-level performance analysis.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Paginated method list (page, size)</li>
 *   <li>Sorted by selfDurationMs (descending)</li>
 *   <li>Includes method information (name, class, parameters, duration)</li>
 *   <li>Calculates self-duration percentage</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
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
     * Retrieves paginated list of methods for a Try, sorted by selfDurationMs (descending).
     * <p>
     * This method:
     * <ol>
     *   <li>Retrieves trace data from Tempo using tryId</li>
     *   <li>Builds hierarchical trace tree</li>
     *   <li>Flattens tree into list of spans</li>
     *   <li>Sorts by selfDurationMs (descending)</li>
     *   <li>Applies pagination</li>
     *   <li>Converts spans to method information</li>
     * </ol>
     *
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @param page Page number (0-based, must be non-negative)
     * @param size Page size (must be between 1 and 100)
     * @return Paginated method list response with sorted methods
     * @throws Exception if retrieval fails
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
     * Builds an empty response when no trace data is available.
     *
     * @param tryId Try session ID
     * @param page Page number
     * @param size Page size
     * @return Empty method list response with zero counts
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
     * Converts SpanNode to MethodInfo for response.
     * <p>
     * Extracts method information including span ID, name, class name,
     * method name, parameters, duration, and percentage.
     *
     * @param spanNode Span node to convert
     * @return Method information DTO
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
                .name(spanNode.getName())
                .className(spanNode.getClassName())
                .methodName(spanNode.getMethodName())
                .parameters(parameters)
                .selfDurationMs(spanNode.getSelfDurationMs())
                .selfPercentage(spanNode.getSelfPercentage())
                .build();
    }
}
