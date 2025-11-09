package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.ui.rest.tryit.dto.TrySummaryResponse;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
import kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus;
import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
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
@DisplayName("TrySummaryService 테스트")
class TrySummaryServiceTest {

    @Mock
    private TempoClient tempoClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceSpanConverter traceSpanConverter;

    @Mock
    private IssueAnalyzer issueAnalyzer;

    @InjectMocks
    private TrySummaryService trySummaryService;

    private String tryId;
    private String traceId;

    @BeforeEach
    void setUp() {
        tryId = "test-try-id";
        traceId = "test-trace-id";
    }

    @Test
    @DisplayName("Tempo가 비활성화된 경우 PENDING 상태 반환")
    void getSummary_TempoDisabled_ReturnsPending() {
        // given
        when(tempoClient.isEnabled()).thenReturn(false);

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertEquals(AnalysisStatus.PENDING, response.getStatus());
        assertEquals(0, response.getSpanCount());
        assertEquals(0, response.getIssueCount());

        verify(tempoClient, never()).pollForTrace(anyString());
    }

    @Test
    @DisplayName("Trace를 찾지 못한 경우 PENDING 상태 반환")
    void getSummary_TraceNotFound_ReturnsPending() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(null);

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertEquals(AnalysisStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("Trace 데이터가 null인 경우 PENDING 상태 반환")
    void getSummary_TraceDataNull_ReturnsPending() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(null);

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertNull(response.getTraceId());
        assertEquals(AnalysisStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("정상적으로 summary 조회 성공")
    void getSummary_Success() throws Exception {
        // given
        String traceDataJson = "{\"batches\":[]}";
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        List<TraceSpanInfo> spans = createTestSpans();
        List<Issue> issues = createTestIssues();

        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(traceDataJson);
        when(objectMapper.readValue(traceDataJson, TraceDTO.class)).thenReturn(traceData);
        when(traceSpanConverter.convert(traceData)).thenReturn(spans);
        when(issueAnalyzer.analyze(spans, 100L)).thenReturn(issues);

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertEquals(traceId, response.getTraceId());
        assertEquals(AnalysisStatus.COMPLETED, response.getStatus());
        assertEquals(200, response.getStatusCode());
        assertEquals(100L, response.getTotalDurationMs());
        assertEquals(1, response.getSpanCount());
        assertEquals(1, response.getIssueCount());

        verify(tempoClient).pollForTrace(contains(tryId));
        verify(traceSpanConverter).convert(traceData);
        verify(issueAnalyzer).analyze(spans, 100L);
    }

    @Test
    @DisplayName("예외 발생 시 FAILED 상태 반환")
    void getSummary_Exception_ReturnsFailed() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenThrow(new RuntimeException("Test exception"));

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertEquals(AnalysisStatus.FAILED, response.getStatus());
        assertNotNull(response.getError());
        assertTrue(response.getError().contains("Failed to retrieve summary"));
        assertEquals(0, response.getSpanCount());
        assertEquals(0, response.getIssueCount());
    }

    @Test
    @DisplayName("HTTP status code 추출 테스트")
    void getSummary_ExtractHttpStatusCode() throws Exception {
        // given
        String traceDataJson = "{\"batches\":[]}";
        TraceDTO traceData = new TraceDTO();
        traceData.setBatches(new ArrayList<>());

        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("http.status_code", "404");
        TraceSpanInfo span = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("http get /api/orders/123")
                .kind("SERVER")
                .startTimeNanos(1000_000_000L) // 1 second in nanoseconds
                .endTimeNanos(1100_000_000L) // 1.1 seconds
                .durationNanos(100_000_000L) // 100ms in nanoseconds
                .durationMs(100L)
                .attributes(attributes)
                .build();

        List<TraceSpanInfo> spans = List.of(span);
        List<Issue> issues = new ArrayList<>();

        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(traceDataJson);
        when(objectMapper.readValue(traceDataJson, TraceDTO.class)).thenReturn(traceData);
        when(traceSpanConverter.convert(traceData)).thenReturn(spans);
        when(issueAnalyzer.analyze(spans, 100L)).thenReturn(issues);

        // when
        TrySummaryResponse response = trySummaryService.getSummary(tryId);

        // then
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
    }

    private List<TraceSpanInfo> createTestSpans() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("http.status_code", "200");
        TraceSpanInfo span1 = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("http get /api/orders")
                .kind("SERVER")
                .startTimeNanos(1000_000_000L) // 1 second in nanoseconds
                .endTimeNanos(1100_000_000L) // 1.1 seconds
                .durationNanos(100_000_000L) // 100ms in nanoseconds
                .durationMs(100L)
                .attributes(attributes)
                .build();

        return List.of(span1);
    }

    private List<Issue> createTestIssues() {
        Issue issue1 = Issue.builder()
                .type(Issue.Type.SLOW_SPAN)
                .severity(Issue.Severity.HIGH)
                .summary("Span takes 50.0% of total time (50ms)")
                .spanName("OrderController.getOrder")
                .durationMs(50L)
                .evidence(List.of("duration: 50ms", "kind: SERVER", "name: OrderController.getOrder"))
                .recommendation("Review method implementation for optimization")
                .build();

        return List.of(issue1);
    }
}
