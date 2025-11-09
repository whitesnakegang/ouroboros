package kr.co.ouroboros.core.rest.tryit.trace.converter;

import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TraceSpanConverter 테스트")
class TraceSpanConverterTest {

    private TraceSpanConverter traceSpanConverter;

    @BeforeEach
    void setUp() {
        traceSpanConverter = new TraceSpanConverter();
    }

    @Test
    @DisplayName("null TraceDTO는 빈 리스트 반환")
    void convert_NullTraceDTO_ReturnsEmptyList() {
        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(null);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("빈 batches 리스트는 빈 리스트 반환")
    void convert_EmptyBatches_ReturnsEmptyList() {
        // given
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("null batches는 빈 리스트 반환")
    void convert_NullBatches_ReturnsEmptyList() {
        // given
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(null);

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("정상적인 span 변환")
    void convert_ValidSpan_ConvertsCorrectly() {
        // given
        TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
        spanDTO.setTraceId("trace1");
        spanDTO.setSpanId("span1");
        spanDTO.setParentSpanId(null);
        spanDTO.setName("TestSpan");
        spanDTO.setKind("SPAN_KIND_SERVER");
        spanDTO.setStartTimeUnixNano(1000L);
        spanDTO.setEndTimeUnixNano(1100L);
        spanDTO.setDurationNanos(100_000_000L); // 100ms in nanoseconds

        TraceDTO.AttributeDTO attr1 = new TraceDTO.AttributeDTO();
        attr1.setKey("key1");
        TraceDTO.ValueDTO value1 = new TraceDTO.ValueDTO();
        value1.setStringValue("value1");
        attr1.setValue(value1);
        spanDTO.setAttributes(List.of(attr1));

        TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
        scopeSpan.setSpans(List.of(spanDTO));

        TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
        batch.setScopeSpans(List.of(scopeSpan));

        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(List.of(batch));

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        TraceSpanInfo span = result.get(0);
        assertEquals("span1", span.getSpanId());
        assertNull(span.getParentSpanId());
        assertEquals("TestSpan", span.getName());
        assertEquals("SERVER", span.getKind());
        assertEquals(1000L, span.getStartTimeNanos());
        assertEquals(1100L, span.getEndTimeNanos());
        assertEquals(100_000_000L, span.getDurationNanos());
        assertEquals(100L, span.getDurationMs());
        assertNotNull(span.getAttributes());
        assertEquals("value1", span.getAttributes().get("key1"));
    }

    @Test
    @DisplayName("durationNanos가 없는 경우 자동 계산")
    void convert_MissingDurationNanos_CalculatesDuration() {
        // given
        TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
        spanDTO.setTraceId("trace1");
        spanDTO.setSpanId("span1");
        spanDTO.setParentSpanId(null);
        spanDTO.setName("TestSpan");
        spanDTO.setKind("SPAN_KIND_INTERNAL");
        spanDTO.setStartTimeUnixNano(1000L);
        spanDTO.setEndTimeUnixNano(1100L);
        spanDTO.setDurationNanos(null);
        spanDTO.setAttributes(new ArrayList<>());

        TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
        scopeSpan.setSpans(List.of(spanDTO));

        TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
        batch.setScopeSpans(List.of(scopeSpan));

        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(List.of(batch));

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        TraceSpanInfo span = result.get(0);
        assertEquals(100L, span.getDurationNanos()); // 1100 - 1000
        assertEquals(0L, span.getDurationMs()); // 100 nanos / 1_000_000 = 0ms (too small)
    }

    @Test
    @DisplayName("span kind 매핑 테스트")
    void convert_SpanKindMapping() {
        // given
        TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
        spanDTO.setTraceId("trace1");
        spanDTO.setSpanId("span1");
        spanDTO.setParentSpanId(null);
        spanDTO.setName("TestSpan");
        spanDTO.setKind("SPAN_KIND_CLIENT");
        spanDTO.setStartTimeUnixNano(1000L);
        spanDTO.setEndTimeUnixNano(1100L);
        spanDTO.setDurationNanos(100L);
        spanDTO.setAttributes(new ArrayList<>());

        TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
        scopeSpan.setSpans(List.of(spanDTO));

        TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
        batch.setScopeSpans(List.of(scopeSpan));

        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(List.of(batch));

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CLIENT", result.get(0).getKind());
    }

    @Test
    @DisplayName("다양한 attribute 타입 변환 테스트")
    void convert_VariousAttributeTypes() {
        // given
        TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
        spanDTO.setTraceId("trace1");
        spanDTO.setSpanId("span1");
        spanDTO.setParentSpanId(null);
        spanDTO.setName("TestSpan");
        spanDTO.setKind("SPAN_KIND_INTERNAL");
        spanDTO.setStartTimeUnixNano(1000L);
        spanDTO.setEndTimeUnixNano(1100L);
        spanDTO.setDurationNanos(100L);

        List<TraceDTO.AttributeDTO> attributes = new ArrayList<>();
        
        // String value
        TraceDTO.AttributeDTO stringAttr = new TraceDTO.AttributeDTO();
        stringAttr.setKey("stringKey");
        TraceDTO.ValueDTO stringValue = new TraceDTO.ValueDTO();
        stringValue.setStringValue("stringValue");
        stringAttr.setValue(stringValue);
        attributes.add(stringAttr);

        // Int value
        TraceDTO.AttributeDTO intAttr = new TraceDTO.AttributeDTO();
        intAttr.setKey("intKey");
        TraceDTO.ValueDTO intValue = new TraceDTO.ValueDTO();
        intValue.setIntValue(42L);
        intAttr.setValue(intValue);
        attributes.add(intAttr);

        // Double value
        TraceDTO.AttributeDTO doubleAttr = new TraceDTO.AttributeDTO();
        doubleAttr.setKey("doubleKey");
        TraceDTO.ValueDTO doubleValue = new TraceDTO.ValueDTO();
        doubleValue.setDoubleValue(3.14);
        doubleAttr.setValue(doubleValue);
        attributes.add(doubleAttr);

        // Bool value
        TraceDTO.AttributeDTO boolAttr = new TraceDTO.AttributeDTO();
        boolAttr.setKey("boolKey");
        TraceDTO.ValueDTO boolValue = new TraceDTO.ValueDTO();
        boolValue.setBoolValue(true);
        boolAttr.setValue(boolValue);
        attributes.add(boolAttr);

        spanDTO.setAttributes(attributes);

        TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
        scopeSpan.setSpans(List.of(spanDTO));

        TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
        batch.setScopeSpans(List.of(scopeSpan));

        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(List.of(batch));

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        TraceSpanInfo span = result.get(0);
        assertNotNull(span.getAttributes());
        assertEquals("stringValue", span.getAttributes().get("stringKey"));
        assertEquals("42", span.getAttributes().get("intKey"));
        assertEquals("3.14", span.getAttributes().get("doubleKey"));
        assertEquals("true", span.getAttributes().get("boolKey"));
    }

    @Test
    @DisplayName("null kind는 INTERNAL로 매핑")
    void convert_NullKind_MapsToInternal() {
        // given
        TraceDTO.SpanDTO spanDTO = new TraceDTO.SpanDTO();
        spanDTO.setTraceId("trace1");
        spanDTO.setSpanId("span1");
        spanDTO.setParentSpanId(null);
        spanDTO.setName("TestSpan");
        spanDTO.setKind(null);
        spanDTO.setStartTimeUnixNano(1000L);
        spanDTO.setEndTimeUnixNano(1100L);
        spanDTO.setDurationNanos(100L);
        spanDTO.setAttributes(new ArrayList<>());

        TraceDTO.ScopeSpanDTO scopeSpan = new TraceDTO.ScopeSpanDTO();
        scopeSpan.setSpans(List.of(spanDTO));

        TraceDTO.BatchDTO batch = new TraceDTO.BatchDTO();
        batch.setScopeSpans(List.of(scopeSpan));

        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(List.of(batch));

        // when
        List<TraceSpanInfo> result = traceSpanConverter.convert(traceData);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("INTERNAL", result.get(0).getKind());
    }
}
