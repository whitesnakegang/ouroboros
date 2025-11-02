package kr.co.ouroboros.core.rest.tryit.trace.converter;

import kr.co.ouroboros.core.rest.tryit.tempo.dto.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Converts TraceDTO from Tempo to TraceSpanInfo intermediate representation.
 */
@Slf4j
@Component
public class TraceSpanConverter {
    
    /**
     * Converts TraceDTO to list of TraceSpanInfo.
     * 
     * @param traceData Trace data from Tempo
     * @return List of span information
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
     * Maps span kind from Tempo format to our internal format.
     * 
     * @param kind Span kind from Tempo
     * @return Internal span kind representation
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

