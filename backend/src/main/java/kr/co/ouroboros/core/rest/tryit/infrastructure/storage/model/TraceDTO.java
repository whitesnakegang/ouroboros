package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Trace data transfer object (DTO) for trace data.
 * <p>
 * This class represents the trace structure following the OpenTelemetry trace data model
 * with batches, resource spans, and individual spans. It is used as a common format
 * for trace data across different storage backends (Tempo, in-memory, database, etc.).
 * <p>
 * <b>Structure:</b>
 * <ul>
 *   <li>{@link BatchDTO} - Contains resource information and scope spans</li>
 *   <li>{@link ScopeSpanDTO} - Contains scope and list of spans</li>
 *   <li>{@link SpanDTO} - Individual span with IDs, timestamps, attributes</li>
 *   <li>{@link AttributeDTO} - Key-value attributes for spans/resource</li>
 * </ul>
 * <p>
 * This DTO is used by {@link kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter}
 * to convert trace data to internal {@link kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo} format.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Data
public class TraceDTO {
    
    @JsonProperty("batches")
    private List<BatchDTO> batches;
    
    /**
     * Batch of trace data.
     * <p>
     * Represents a batch of spans with resource information.
     * Contains resource attributes and list of scope spans.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class BatchDTO {
        
        @JsonProperty("resource")
        private ResourceDTO resource;
        
        @JsonProperty("scopeSpans")
        private List<ScopeSpanDTO> scopeSpans;
    }
    
    /**
     * Resource information for trace batch.
     * <p>
     * Contains resource-level attributes that apply to all spans in the batch.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class ResourceDTO {
        
        @JsonProperty("attributes")
        private List<AttributeDTO> attributes;
    }
    
    /**
     * Scope spans containing instrumentation scope and list of spans.
     * <p>
     * Groups spans by their instrumentation scope (library/component name).
     * Contains scope metadata and list of actual spans.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class ScopeSpanDTO {
        
        @JsonProperty("scope")
        private ScopeDTO scope;
        
        @JsonProperty("spans")
        private List<SpanDTO> spans;
    }
    
    /**
     * Instrumentation scope information.
     * <p>
     * Contains metadata about the instrumentation library/component
     * that created the spans.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class ScopeDTO {
        
        @JsonProperty("name")
        private String name;
    }
    
    /**
     * Individual span data.
     * <p>
     * Represents a single span in the trace with:
     * <ul>
     *   <li>Span IDs (traceId, spanId, parentSpanId)</li>
     *   <li>Span name and kind</li>
     *   <li>Timestamps (start, end, duration in nanoseconds)</li>
     *   <li>Attributes (key-value pairs)</li>
     * </ul>
     * <p>
     * This is the core span data structure used to represent
     * method calls and operations in a distributed trace.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class SpanDTO {
        
        @JsonProperty("traceId")
        private String traceId;
        
        @JsonProperty("spanId")
        private String spanId;
        
        @JsonProperty("parentSpanId")
        private String parentSpanId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("kind")
        private String kind;
        
        @JsonProperty("startTimeUnixNano")
        private Long startTimeUnixNano;
        
        @JsonProperty("endTimeUnixNano")
        private Long endTimeUnixNano;
        
        @JsonProperty("durationNanos")
        private Long durationNanos;
        
        @JsonProperty("attributes")
        private List<AttributeDTO> attributes;
    }
    
    /**
     * Attribute key-value pair for spans or resource.
     * <p>
     * Represents metadata attributes attached to spans or resource.
     * Contains a key and a value (which can be string, int, double, or bool).
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class AttributeDTO {
        
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("value")
        private ValueDTO value;
    }
    
    /**
     * Attribute value container supporting multiple types.
     * <p>
     * Represents attribute values that can be one of:
     * <ul>
     *   <li>String value (stringValue)</li>
     *   <li>Integer value (intValue)</li>
     *   <li>Double value (doubleValue)</li>
     *   <li>Boolean value (boolValue)</li>
     * </ul>
     * <p>
     * Only one value type should be set per attribute.
     *
     * @author Ouroboros Team
     * @since 0.0.1
     */
    @Data
    public static class ValueDTO {
        
        @JsonProperty("stringValue")
        private String stringValue;
        
        @JsonProperty("intValue")
        private Long intValue;
        
        @JsonProperty("doubleValue")
        private Double doubleValue;
        
        @JsonProperty("boolValue")
        private Boolean boolValue;
    }
}

