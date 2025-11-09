package kr.co.ouroboros.core.rest.tryit.service;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceDataRetriever;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.util.SpanFlattener;
import kr.co.ouroboros.core.rest.tryit.trace.util.TraceDurationCalculator;
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
    
    private final TraceDataRetriever traceDataRetriever;
    private final TraceTreeBuilder traceTreeBuilder;
    private final SpanFlattener spanFlattener;
    
    /**
     * Retrieves a paginated list of methods for the given Try, sorted by `selfDurationMs` in descending order.
     * <p>
     * Queries Tempo using the trace query format: {@code { span.ouro.try_id = "tryId" }}.
     * If Tempo is disabled or a trace for the given tryId is not found (or cannot be retrieved), returns an empty response
     * with zeroed metadata and an empty method list.
     *
     * @param tryIdStr Try session ID (expected to be a valid UUID)
     * @param page     Page number (0-based; must be >= 0)
     * @param size     Page size (must be between 1 and 100)
     * @return         A TryMethodListResponse containing method entries sorted by selfDurationMs and pagination metadata;
     *                 `traceId` may be null when no trace is available.
     */
    public TryMethodListResponse getMethodList(String tryIdStr, int page, int size) {
        log.info("Retrieving method list for tryId: {}, page: {}, size: {}", tryIdStr, page, size);
        
        return traceDataRetriever.getTraceData(tryIdStr)
                .map(result -> {
                    List<kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo> spans = result.getSpans();
                    String traceId = result.getTraceId();
                    
                    // Calculate total duration
                    long totalDurationMs = TraceDurationCalculator.calculateTotalDuration(spans);
                    
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
                })
                .orElse(buildEmptyResponse(tryIdStr, page, size));
    }
    
    /**
     * Produce a TryMethodListResponse representing an empty result for the given try session.
     *
     * @param tryId the try session identifier to include in the response
     * @param page the requested page number to include in the response metadata
     * @param size the requested page size to include in the response metadata
     * @return a TryMethodListResponse with {@code traceId} set to {@code null}, {@code totalDurationMs} = 0,
     *         {@code totalCount} = 0, {@code hasMore} = {@code false}, and an empty {@code methods} list
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
     * Create a MethodInfo DTO representing the given span node.
     *
     * @param spanNode the span node to convert into a MethodInfo
     * @return a MethodInfo populated with spanId, name, className, methodName, parameters, selfDurationMs, and selfPercentage
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
