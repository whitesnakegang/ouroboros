package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.ui.rest.tryit.dto.TryTraceResponse;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TryTraceService 테스트")
class TryTraceServiceTest {

    @Mock
    private TempoClient tempoClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceSpanConverter traceSpanConverter;

    @Mock
    private TraceTreeBuilder traceTreeBuilder;

    @InjectMocks
    private TryTraceService tryTraceService;

    private String tryId;
    private String traceId;

    @BeforeEach
    void setUp() {
        tryId = "test-try-id";
        traceId = "test-trace-id";
    }

    @Test
    @DisplayName("Tempo가 비활성화된 경우 빈 응답 반환")
    void getTrace_TempoDisabled_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(false);

        // when
        TryTraceResponse response = tryTraceService.getTrace(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertEquals(0L, response.getTotalDurationMs());
        assertTrue(response.getSpans().isEmpty());

        verify(tempoClient, never()).pollForTrace(anyString());
    }

    @Test
    @DisplayName("Trace를 찾지 못한 경우 빈 응답 반환")
    void getTrace_TraceNotFound_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(null);

        // when
        TryTraceResponse response = tryTraceService.getTrace(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getSpans().isEmpty());
    }

    @Test
    @DisplayName("Trace 데이터가 null인 경우 빈 응답 반환")
    void getTrace_TraceDataNull_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(null);

        // when
        TryTraceResponse response = tryTraceService.getTrace(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getSpans().isEmpty());
    }

    @Test
    @DisplayName("정상적으로 trace 조회 성공")
    void getTrace_Success() throws Exception {
        // given
        String traceDataJson = "{\"batches\":[]}";
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        List<TraceSpanInfo> spans = createTestSpans();
        List<SpanNode> spanTree = createTestSpanTree();

        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(traceDataJson);
        when(objectMapper.readValue(traceDataJson, TraceDTO.class)).thenReturn(traceData);
        when(traceSpanConverter.convert(traceData)).thenReturn(spans);
        when(traceTreeBuilder.buildTree(eq(spans), anyLong())).thenReturn(spanTree);

        // when
        TryTraceResponse response = tryTraceService.getTrace(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertEquals(traceId, response.getTraceId());
        assertTrue(response.getTotalDurationMs() > 0);
        assertEquals(1, response.getSpans().size());
        assertEquals("OrderController.getOrder", response.getSpans().get(0).getName());

        verify(tempoClient).pollForTrace(contains(tryId));
        verify(traceSpanConverter).convert(traceData);
        verify(traceTreeBuilder).buildTree(eq(spans), anyLong());
    }

    @Test
    @DisplayName("예외 발생 시 빈 응답 반환")
    void getTrace_Exception_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenThrow(new RuntimeException("Test exception"));

        // when
        TryTraceResponse response = tryTraceService.getTrace(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getSpans().isEmpty());
    }

    private List<TraceSpanInfo> createTestSpans() {
        TraceSpanInfo span1 = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("OrderController.getOrder")
                .kind("SERVER")
                .startTimeNanos(1000_000_000L) // 1 second in nanoseconds
                .endTimeNanos(1100_000_000L) // 1.1 seconds
                .durationNanos(100_000_000L) // 100ms in nanoseconds
                .durationMs(100L)
                .attributes(new HashMap<>())
                .build();

        return List.of(span1);
    }

    private List<SpanNode> createTestSpanTree() {
        SpanNode node1 = SpanNode.builder()
                .spanId("span1")
                .name("OrderController.getOrder")
                .className("OrderController")
                .methodName("getOrder")
                .durationMs(100L)
                .selfDurationMs(100L)
                .percentage(100.0)
                .selfPercentage(100.0)
                .kind("SERVER")
                .children(new ArrayList<>())
                .parameters(List.of())
                .build();

        return List.of(node1);
    }
}
