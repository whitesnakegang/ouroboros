package kr.co.ouroboros.core.rest.tryit.tempo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Trace data from Tempo.
 * Represents the parsed trace structure.
 */
@Data
public class TraceDTO {
    
    @JsonProperty("batches")
    private List<BatchDTO> batches;
    
    @Data
    public static class BatchDTO {
        
        @JsonProperty("resource")
        private ResourceDTO resource;
        
        @JsonProperty("scopeSpans")
        private List<ScopeSpanDTO> scopeSpans;
    }
    
    @Data
    public static class ResourceDTO {
        
        @JsonProperty("attributes")
        private List<AttributeDTO> attributes;
    }
    
    @Data
    public static class ScopeSpanDTO {
        
        @JsonProperty("scope")
        private ScopeDTO scope;
        
        @JsonProperty("spans")
        private List<SpanDTO> spans;
    }
    
    @Data
    public static class ScopeDTO {
        
        @JsonProperty("name")
        private String name;
    }
    
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
    
    @Data
    public static class AttributeDTO {
        
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("value")
        private ValueDTO value;
    }
    
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

