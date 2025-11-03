package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.api.dto.TryMethodListResponse;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.builder.TraceTreeBuilder;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.trace.util.SpanFlattener;
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
@DisplayName("TryMethodListService 테스트")
class TryMethodListServiceTest {

    @Mock
    private TempoClient tempoClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceSpanConverter traceSpanConverter;

    @Mock
    private TraceTreeBuilder traceTreeBuilder;

    @Mock
    private SpanFlattener spanFlattener;

    @InjectMocks
    private TryMethodListService tryMethodListService;

    private String tryId;
    private String traceId;

    @BeforeEach
    void setUp() {
        tryId = "test-try-id";
        traceId = "test-trace-id";
    }

    @Test
    @DisplayName("Tempo가 비활성화된 경우 빈 응답 반환")
    void getMethodList_TempoDisabled_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(false);

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertEquals(0L, response.getTotalDurationMs());
        assertEquals(0, response.getTotalCount());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertFalse(response.getHasMore());
        assertTrue(response.getMethods().isEmpty());

        verify(tempoClient, never()).pollForTrace(anyString());
    }

    @Test
    @DisplayName("Trace를 찾지 못한 경우 빈 응답 반환")
    void getMethodList_TraceNotFound_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(null);

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getMethods().isEmpty());

        verify(tempoClient).pollForTrace(contains(tryId));
    }

    @Test
    @DisplayName("Trace 데이터가 null인 경우 빈 응답 반환")
    void getMethodList_TraceDataNull_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(null);

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getMethods().isEmpty());
    }

    @Test
    @DisplayName("정상적으로 메서드 목록 조회 성공")
    void getMethodList_Success() throws Exception {
        // given
        String traceDataJson = "{\"batches\":[]}";
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        List<TraceSpanInfo> spans = createTestSpans();
        List<SpanNode> spanTree = createTestSpanTree();
        List<SpanNode> flatSpans = List.of(spanTree.get(0));

        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(traceDataJson);
        when(objectMapper.readValue(traceDataJson, TraceDTO.class)).thenReturn(traceData);
        when(traceSpanConverter.convert(traceData)).thenReturn(spans);
        when(traceTreeBuilder.buildTree(eq(spans), anyLong())).thenReturn(spanTree);
        when(spanFlattener.flatten(spanTree)).thenReturn(flatSpans);

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertEquals(traceId, response.getTraceId());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertFalse(response.getHasMore());

        verify(tempoClient).pollForTrace(contains(tryId));
        verify(traceSpanConverter).convert(traceData);
        verify(traceTreeBuilder).buildTree(eq(spans), anyLong());
        verify(spanFlattener).flatten(spanTree);
    }

    @Test
    @DisplayName("페이지네이션 테스트 - hasMore가 true인 경우")
    void getMethodList_Pagination_HasMore() throws Exception {
        // given
        String traceDataJson = "{\"batches\":[]}";
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        List<TraceSpanInfo> spans = createTestSpans();
        List<SpanNode> spanTree = createTestSpanTree();
        List<SpanNode> flatSpans = createMultipleSpanNodes(10);

        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(traceDataJson);
        when(objectMapper.readValue(traceDataJson, TraceDTO.class)).thenReturn(traceData);
        when(traceSpanConverter.convert(traceData)).thenReturn(spans);
        when(traceTreeBuilder.buildTree(eq(spans), anyLong())).thenReturn(spanTree);
        when(spanFlattener.flatten(spanTree)).thenReturn(flatSpans);

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(10, response.getTotalCount());
        assertEquals(5, response.getMethods().size());
        assertTrue(response.getHasMore());
        assertEquals(traceId, response.getTraceId());
    }

    @Test
    @DisplayName("예외 발생 시 빈 응답 반환")
    void getMethodList_Exception_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenThrow(new RuntimeException("Test exception"));

        // when
        TryMethodListResponse response = tryMethodListService.getMethodList(tryId, 0, 5);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertTrue(response.getMethods().isEmpty());
    }

    @Test
    @DisplayName("SpanNode name 필드가 존재하는지 확인")
    void spanNode_HasNameField() {
        // given
        SpanNode spanNode = SpanNode.builder()
                .spanId("span1")
                .name("OrderController.getOrder")
                .className("OrderController")
                .methodName("getOrder")
                .selfDurationMs(50L)
                .selfPercentage(50.0)
                .parameters(List.of())
                .build();

        // when & then
        assertNotNull(spanNode.getName());
        assertEquals("OrderController.getOrder", spanNode.getName());
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

    private List<SpanNode> createMultipleSpanNodes(int count) {
        List<SpanNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            nodes.add(SpanNode.builder()
                    .spanId("span" + i)
                    .name("Class" + i + ".method" + i)
                    .className("Class" + i)
                    .methodName("method" + i)
                    .durationMs((long) (100 - i * 10))
                    .selfDurationMs((long) (100 - i * 10))
                    .percentage(100.0 - i * 10)
                    .selfPercentage(100.0 - i * 10)
                    .kind("INTERNAL")
                    .children(new ArrayList<>())
                    .parameters(List.of())
                    .build());
        }
        return nodes;
    }
}
