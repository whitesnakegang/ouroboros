package kr.co.ouroboros.core.rest.tryit.trace.converter;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converts TraceDTO from Tempo to TraceSpanInfo intermediate representation.
 * <p>
 * This component converts trace data from Tempo's TraceDTO format to
 * internal TraceSpanInfo representation for processing.
 * <p>
 * <b>Conversion Process:</b>
 * <ul>
 *   <li>Extracts spans from TraceDTO batches</li>
 *   <li>Converts span timestamps and durations</li>
 *   <li>Maps span kinds to internal format</li>
 *   <li>Extracts span attributes into map</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class TraceSpanConverter {
    
    /**
     * Converts TraceDTO to list of TraceSpanInfo.
     * <p>
     * Extracts all spans from TraceDTO batches and converts them to
     * TraceSpanInfo format, including:
     * <ul>
     *   <li>Span IDs and parent relationships</li>
     *   <li>Timestamps (start, end, duration)</li>
     *   <li>Span kind and name</li>
     *   <li>Attributes as key-value map</li>
     * </ul>
     *
     * @param traceData Trace data from Tempo in TraceDTO format
     * @return List of span information in TraceSpanInfo format
     */
    public List<TraceSpanInfo> convert(TraceDTO traceData) {
        if (traceData == null || traceData.getBatches() == null) {
            log.debug("TraceData or batches is null");
            return new ArrayList<>();
        }
        
        List<TraceSpanInfo> spans = new ArrayList<>();
        
        for (TraceDTO.BatchDTO batch : traceData.getBatches()) {
            if (batch.getScopeSpans() == null) {
                continue;
            }
            
            for (TraceDTO.ScopeSpanDTO scopeSpan : batch.getScopeSpans()) {
                if (scopeSpan.getSpans() == null) {
                    continue;
                }
                
                for (TraceDTO.SpanDTO span : scopeSpan.getSpans()) {
                    TraceSpanInfo info = TraceSpanInfo.builder()
                            .spanId(span.getSpanId())
                            .parentSpanId(span.getParentSpanId())
                            .name(span.getName())
                            .kind(mapSpanKind(span.getKind()))
                            .startTimeNanos(span.getStartTimeUnixNano())
                            .endTimeNanos(span.getEndTimeUnixNano())
                            .durationNanos(span.getDurationNanos())
                            .build();
                    
                    // Compute duration if not provided
                    if (info.getDurationNanos() == null && info.getStartTimeNanos() != null && info.getEndTimeNanos() != null) {
                        long durationNanos = Math.max(0L, info.getEndTimeNanos() - info.getStartTimeNanos());
                        info.setDurationNanos(durationNanos);
                    }
                    
                    // Convert duration to milliseconds
                    long durationMs = info.getDurationNanos() != null ? info.getDurationNanos() / 1_000_000 : 0L;
                    info.setDurationMs(durationMs);
                    
                    // Extract attributes into a map for easier access
                    info.setAttributes(extractAttributes(span));
                    
                    spans.add(info);
                }
            }
        }
        
        log.debug("Converted {} spans from TraceDTO", spans.size());
        return spans;
    }
    
    /**
     * Extracts attributes from span into a map.
     * <p>
     * Converts span attributes from TraceDTO format to a simple key-value map.
     * Handles various attribute value types (string, int, double, bool).
     *
     * @param span Span DTO from Tempo containing attributes
     * @return Map of attribute keys to string values
     */
    private java.util.Map<String, String> extractAttributes(TraceDTO.SpanDTO span) {
        java.util.Map<String, String> attributes = new HashMap<>();
        
        if (span.getAttributes() == null) {
            return attributes;
        }
        
        for (TraceDTO.AttributeDTO attr : span.getAttributes()) {
            if (attr.getKey() == null || attr.getValue() == null) {
                continue;
            }
            
            String value = attr.getValue().getStringValue();
            if (value == null && attr.getValue().getIntValue() != null) {
                value = String.valueOf(attr.getValue().getIntValue());
            }
            if (value == null && attr.getValue().getDoubleValue() != null) {
                value = String.valueOf(attr.getValue().getDoubleValue());
            }
            if (value == null && attr.getValue().getBoolValue() != null) {
                value = String.valueOf(attr.getValue().getBoolValue());
            }
            
            if (value != null) {
                attributes.put(attr.getKey(), value);
            }
        }
        
        return attributes;
    }
    
    /**
     * Maps span kind from Tempo format to internal format.
     * <p>
     * Converts OpenTelemetry span kind strings to simplified internal format:
     * <ul>
     *   <li>SPAN_KIND_UNSPECIFIED → UNSPECIFIED</li>
     *   <li>SPAN_KIND_INTERNAL → INTERNAL</li>
     *   <li>SPAN_KIND_SERVER → SERVER</li>
     *   <li>SPAN_KIND_CLIENT → CLIENT</li>
     *   <li>SPAN_KIND_PRODUCER → PRODUCER</li>
     *   <li>SPAN_KIND_CONSUMER → CONSUMER</li>
     *   <li>Default → INTERNAL</li>
     * </ul>
     *
     * @param kind Span kind string from Tempo (OpenTelemetry format)
     * @return Internal span kind representation (simplified format)
     */
    private String mapSpanKind(String kind) {
        if (kind == null) {
            return "INTERNAL";
        }
        
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
}

