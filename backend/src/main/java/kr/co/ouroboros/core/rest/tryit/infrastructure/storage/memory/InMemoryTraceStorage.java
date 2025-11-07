package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.ReadableSpan;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory implementation of TraceStorage interface.
 * <p>
 * This component stores OpenTelemetry spans in memory, organized by tryId.
 * When Tempo is disabled, this storage is used as a fallback.
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Thread-safe span storage using ConcurrentHashMap</li>
 *   <li>Automatic trace grouping by tryId</li>
 *   <li>TraceDTO conversion for compatibility with existing code</li>
 *   <li>Memory-efficient span storage</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class InMemoryTraceStorage implements TraceStorage {
    
    private static final AttributeKey<String> TRY_ID_ATTRIBUTE = AttributeKey.stringKey("ouro.try_id");
    
    /**
     * Storage: tryId -> TraceData
     */
    private final ConcurrentHashMap<String, TraceData> traces = new ConcurrentHashMap<>();
    
    /**
     * Mapping: traceId -> tryId (for reverse lookup)
     */
    private final ConcurrentHashMap<String, String> traceIdToTryId = new ConcurrentHashMap<>();
    
    /**
     * Adds a span to the storage for the given tryId.
     * <p>
     * Extracts tryId from span attributes and stores the span.
     * If the span doesn't have a tryId attribute, it is ignored.
     *
     * @param span The span to store
     */
    public void addSpan(ReadableSpan span) {
        String tryId = span.getAttribute(TRY_ID_ATTRIBUTE);
        if (tryId == null) {
            log.debug("Span does not have tryId attribute, skipping");
            return;
        }
        
        String traceId = span.getSpanContext().getTraceId();
        
        traces.computeIfAbsent(tryId, k -> new TraceData(traceId))
              .addSpan(span);
        
        traceIdToTryId.put(traceId, tryId);
        
        log.debug("Added span to in-memory storage: tryId={}, traceId={}, spanId={}", 
                  tryId, traceId, span.getSpanContext().getSpanId());
    }
    
    /**
     * Retrieves trace data by tryId.
     *
     * @param tryId The try ID to look up
     * @return TraceDTO if found, null otherwise
     */
    public TraceDTO getTraceByTryId(String tryId) {
        TraceData traceData = traces.get(tryId);
        if (traceData == null) {
            return null;
        }
        return traceData.toTraceDTO();
    }
    
    /**
     * Retrieves trace data by traceId.
     *
     * @param traceId The trace ID to look up
     * @return TraceDTO if found, null otherwise
     */
    public TraceDTO getTraceByTraceId(String traceId) {
        String tryId = traceIdToTryId.get(traceId);
        if (tryId == null) {
            return null;
        }
        return getTraceByTryId(tryId);
    }
    
    /**
     * Checks if a trace exists for the given tryId.
     *
     * @param tryId The try ID to check
     * @return true if trace exists, false otherwise
     */
    public boolean hasTrace(String tryId) {
        return traces.containsKey(tryId);
    }
    
    /**
     * Gets the trace ID for a given tryId.
     *
     * @param tryId The try ID to look up
     * @return Trace ID if found, null otherwise
     */
    public String getTraceId(String tryId) {
        TraceData traceData = traces.get(tryId);
        return traceData != null ? traceData.getTraceId() : null;
    }
    
    /**
     * Clears all stored traces (useful for testing or cleanup).
     */
    public void clear() {
        traces.clear();
        traceIdToTryId.clear();
        log.debug("Cleared all in-memory traces");
    }
    
    /**
     * Internal class to hold trace data for a tryId.
     */
    private static class TraceData {
        private final String traceId;
        private final List<ReadableSpan> spans = new CopyOnWriteArrayList<>();
        
        public TraceData(String traceId) {
            this.traceId = traceId;
        }
        
        public void addSpan(ReadableSpan span) {
            spans.add(span);
        }
        
        public String getTraceId() {
            return traceId;
        }
        
        /**
         * Converts stored spans to TraceDTO format.
         */
        public TraceDTO toTraceDTO() {
            TraceDTO traceDTO = new TraceDTO();
            TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
            TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
            
            List<TraceDTO.SpanDTO> spanDTOs = spans.stream()
                    .map(this::convertSpan)
                    .collect(Collectors.toList());
            
            scopeSpan.setSpans(spanDTOs);
            batch.setScopeSpans(List.of(scopeSpan));
            traceDTO.setBatches(List.of(batch));
            
            return traceDTO;
        }
        
        /**
         * Converts ReadableSpan to TraceDTO.SpanDTO.
         */
        private TraceDTO.SpanDTO convertSpan(ReadableSpan span) {
            TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
            
            // Convert to SpanData to access time information
            io.opentelemetry.sdk.trace.data.SpanData spanData = span.toSpanData();
            
            spanDTO.setTraceId(span.getSpanContext().getTraceId());
            spanDTO.setSpanId(span.getSpanContext().getSpanId());
            
            if (span.getParentSpanContext().isValid()) {
                spanDTO.setParentSpanId(span.getParentSpanContext().getSpanId());
            }
            
            spanDTO.setName(span.getName());
            spanDTO.setKind(mapSpanKind(span.getKind()));
            spanDTO.setStartTimeUnixNano(spanData.getStartEpochNanos());
            spanDTO.setEndTimeUnixNano(spanData.getEndEpochNanos());
            
            if (spanData.getStartEpochNanos() != 0 && spanData.getEndEpochNanos() != 0) {
                spanDTO.setDurationNanos(spanData.getEndEpochNanos() - spanData.getStartEpochNanos());
            }
            
            // Convert attributes
            spanDTO.setAttributes(convertAttributes(span.getAttributes()));
            
            return spanDTO;
        }
        
        /**
         * Converts OpenTelemetry Attributes to TraceDTO.AttributeDTO list.
         */
        private List<TraceDTO.AttributeDTO> convertAttributes(Attributes attributes) {
            List<TraceDTO.AttributeDTO> attributeDTOs = new ArrayList<>();
            
            attributes.forEach((key, value) -> {
                TraceDTO.AttributeDTO attrDTO = new TraceDTO.AttributeDTO();
                attrDTO.setKey(key.getKey());
                
                TraceDTO.ValueDTO valueDTO = new TraceDTO.ValueDTO();
                
                if (value instanceof String) {
                    valueDTO.setStringValue((String) value);
                } else if (value instanceof Long) {
                    valueDTO.setIntValue((Long) value);
                } else if (value instanceof Double) {
                    valueDTO.setDoubleValue((Double) value);
                } else if (value instanceof Boolean) {
                    valueDTO.setBoolValue((Boolean) value);
                } else {
                    valueDTO.setStringValue(String.valueOf(value));
                }
                
                attrDTO.setValue(valueDTO);
                attributeDTOs.add(attrDTO);
            });
            
            return attributeDTOs;
        }
        
        /**
         * Maps OpenTelemetry SpanKind to string format.
         */
        private String mapSpanKind(io.opentelemetry.api.trace.SpanKind kind) {
            if (kind == null) {
                return "SPAN_KIND_INTERNAL";
            }
            
            switch (kind) {
                case INTERNAL:
                    return "SPAN_KIND_INTERNAL";
                case SERVER:
                    return "SPAN_KIND_SERVER";
                case CLIENT:
                    return "SPAN_KIND_CLIENT";
                case PRODUCER:
                    return "SPAN_KIND_PRODUCER";
                case CONSUMER:
                    return "SPAN_KIND_CONSUMER";
                default:
                    return "SPAN_KIND_UNSPECIFIED";
            }
        }
    }
}

