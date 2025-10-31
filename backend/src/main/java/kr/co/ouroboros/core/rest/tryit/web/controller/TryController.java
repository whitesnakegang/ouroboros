package kr.co.ouroboros.core.rest.tryit.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.analysis.TraceAnalyzer;
import kr.co.ouroboros.core.rest.tryit.web.dto.TryResultResponse;
import kr.co.ouroboros.core.rest.tryit.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
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
            
            // Calculate total duration
            long totalDurationMs = calculateTotalDuration(traceData);
            
            // Analyze trace and build response
            TryResultResponse response = traceAnalyzer.analyze(traceData, totalDurationMs);
            
            // Set tryId and traceId
            response.setTryId(tryIdStr);
            response.setTraceId(traceId);
            response.setCreatedAt(Instant.now());
            response.setAnalyzedAt(Instant.now());
            Integer statusCode = extractHttpStatusCode(traceData);
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
     * @param traceData Trace data
     * @return Total duration in milliseconds
     */
    private long calculateTotalDuration(TraceDTO traceData) {
        if (traceData == null || traceData.getBatches() == null) {
            return 0;
        }
        
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        
        for (TraceDTO.BatchDTO batch : traceData.getBatches()) {
            if (batch.getScopeSpans() == null) {
                continue;
            }
            
            for (TraceDTO.ScopeSpanDTO scopeSpan : batch.getScopeSpans()) {
                if (scopeSpan.getSpans() == null) {
                    continue;
                }
                
                for (TraceDTO.SpanDTO span : scopeSpan.getSpans()) {
                    if (span.getStartTimeUnixNano() != null && span.getEndTimeUnixNano() != null) {
                        minStart = Math.min(minStart, span.getStartTimeUnixNano());
                        maxEnd = Math.max(maxEnd, span.getEndTimeUnixNano());
                    }
                }
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
    private Integer extractHttpStatusCode(TraceDTO traceData) {
        if (traceData == null || traceData.getBatches() == null) return null;
        for (TraceDTO.BatchDTO batch : traceData.getBatches()) {
            if (batch.getScopeSpans() == null) continue;
            for (TraceDTO.ScopeSpanDTO scopeSpan : batch.getScopeSpans()) {
                if (scopeSpan.getSpans() == null) continue;
                for (TraceDTO.SpanDTO span : scopeSpan.getSpans()) {
                    // Prefer server span or http-named span
                    boolean maybeHttp = (span.getKind() != null && span.getKind().equals("SPAN_KIND_SERVER"))
                            || (span.getName() != null && span.getName().toLowerCase().startsWith("http"));
                    if (!maybeHttp || span.getAttributes() == null) continue;
                    Integer code = null;
                    for (TraceDTO.AttributeDTO attr : span.getAttributes()) {
                        if (attr.getKey() == null || attr.getValue() == null) continue;
                        String key = attr.getKey();
                        if ("http.status_code".equalsIgnoreCase(key) || "status".equalsIgnoreCase(key)) {
                            if (attr.getValue().getIntValue() != null) {
                                code = attr.getValue().getIntValue().intValue();
                            } else if (attr.getValue().getStringValue() != null) {
                                try {
                                    code = Integer.parseInt(attr.getValue().getStringValue());
                                } catch (NumberFormatException ignored) { }
                            }
                        }
                    }
                    if (code != null) return code;
                }
            }
        }
        return null;
    }
}

