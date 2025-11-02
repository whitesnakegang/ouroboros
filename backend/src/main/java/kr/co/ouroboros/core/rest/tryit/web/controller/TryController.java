package kr.co.ouroboros.core.rest.tryit.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.analysis.TraceAnalyzer;
import kr.co.ouroboros.core.rest.tryit.web.dto.TryMethodListResponse;
import kr.co.ouroboros.core.rest.tryit.web.dto.TryResultResponse;
import kr.co.ouroboros.core.rest.tryit.service.TryMethodListService;
import kr.co.ouroboros.core.rest.tryit.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for Try result retrieval.
 * Retrieves Try analysis results for QA analysis.
 * 
 * Note: Session creation is no longer needed. Try requests are identified
 * by X-Ouroboros-Try: on header and tryId is returned in response header.
 */
@Slf4j
@RestController
@RequestMapping("/ouro/tries")
@RequiredArgsConstructor
public class TryController {

    private final TempoClient tempoClient;
    private final TraceAnalyzer traceAnalyzer;
    private final ObjectMapper objectMapper;
    private final TraceSpanConverter traceSpanConverter;
    private final TryMethodListService tryMethodListService;

    /**
     * Retrieves paginated list of methods for a try, sorted by selfDurationMs (descending).
     * 
     * GET /ouro/tries/{tryId}/methods
     * 
     * @param tryIdStr Try session ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return paginated method list
     */
    @GetMapping("/{tryId}/methods")
    public TryMethodListResponse getMethods(
            @PathVariable("tryId") String tryIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        // Validate tryId format
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }
        
        return tryMethodListService.getMethodList(tryIdStr, page, size);
    }
    
    /**
     * Retrieves Try result.
     * 
     * GET /ouro/tries/{tryId}
     * 
     * @param tryIdStr Try session ID
     * @return analysis result
     */
    @GetMapping("/{tryId}")
    public TryResultResponse getResult(@PathVariable("tryId") String tryIdStr) {
        UUID tryId;
        try {
            tryId = UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }
        
        log.info("Retrieving result for tryId: {}", tryId);
        
        // Check if Tempo is enabled
        if (!tempoClient.isEnabled()) {
            log.debug("Tempo is not enabled, returning pending status");
            return TryResultResponse.builder()
                    .tryId(tryIdStr)
                    .status(TryResultResponse.Status.PENDING)
                    .createdAt(Instant.now())
                    .analyzedAt(Instant.now())
                    .totalDurationMs(0L)
                    .spans(new ArrayList<>())
                    .issues(new ArrayList<>())
                    .spanCount(0)
                    .build();
        }
        
        try {
            // Query Tempo for traces with this tryId in baggage
            // OpenTelemetry Baggage is automatically added as span attributes
            String query = String.format("{ span.ouro.try_id = \"%s\" }", tryId);
            String traceId = tempoClient.pollForTrace(query);
            
            if (traceId == null) {
                log.debug("Trace not found in Tempo for tryId: {}", tryId);
                return TryResultResponse.builder()
                        .tryId(tryIdStr)
                        .status(TryResultResponse.Status.PENDING)
                        .createdAt(Instant.now())
                        .analyzedAt(Instant.now())
                        .totalDurationMs(0L)
                        .spans(new ArrayList<>())
                        .issues(new ArrayList<>())
                        .spanCount(0)
                        .build();
            }
            
            // Fetch trace data
            String traceDataJson = tempoClient.getTrace(traceId);
            
            // Parse trace data
            TraceDTO traceData = objectMapper.readValue(traceDataJson, TraceDTO.class);
            
            // Convert to TraceSpanInfo
            List<TraceSpanInfo> spans = traceSpanConverter.convert(traceData);
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(spans);
            
            // Analyze trace and build response
            TryResultResponse response = traceAnalyzer.analyze(traceData, totalDurationMs);
            
            // Set tryId and traceId
            response.setTryId(tryIdStr);
            response.setTraceId(traceId);
            response.setCreatedAt(Instant.now());
            response.setAnalyzedAt(Instant.now());
            Integer statusCode = extractHttpStatusCode(spans);
            response.setStatusCode(statusCode != null ? statusCode : 200);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error retrieving trace for tryId: {}", tryId, e);
            return TryResultResponse.builder()
                    .tryId(tryIdStr)
                    .status(TryResultResponse.Status.FAILED)
                    .createdAt(Instant.now())
                    .analyzedAt(Instant.now())
                    .error("Failed to retrieve trace: " + e.getMessage())
                    .spans(new ArrayList<>())
                    .issues(new ArrayList<>())
                    .spanCount(0)
                    .build();
        }
    }
    
    /**
     * Calculates total duration of the trace.
     * 
     * @param spans List of spans
     * @return Total duration in milliseconds
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
     * Extracts HTTP status code from server span attributes.
     */
    private Integer extractHttpStatusCode(List<TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }
        
        for (TraceSpanInfo span : spans) {
            // Prefer server span or http-named span
            boolean maybeHttp = ("SERVER".equals(span.getKind()))
                    || (span.getName() != null && span.getName().toLowerCase().startsWith("http"));
            
            if (!maybeHttp || span.getAttributes() == null) {
                continue;
            }
            
            // Check common status code attribute keys
            String statusCodeStr = span.getAttributes().get("http.status_code");
            if (statusCodeStr == null) {
                statusCodeStr = span.getAttributes().get("status");
            }
            
            if (statusCodeStr != null) {
                try {
                    return Integer.parseInt(statusCodeStr);
                } catch (NumberFormatException ignored) {
                    // Continue to next span
                }
            }
        }
        
        return null;
    }
}

