package kr.co.ouroboros.core.rest.tryit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.ouroboros.core.rest.tryit.api.dto.TryIssuesResponse;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.TempoClient;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.model.TraceDTO;
import kr.co.ouroboros.core.rest.tryit.trace.analyzer.IssueAnalyzer;
import kr.co.ouroboros.core.rest.tryit.trace.converter.TraceSpanConverter;
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
@DisplayName("TryIssuesService 테스트")
class TryIssuesServiceTest {

    @Mock
    private TempoClient tempoClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TraceSpanConverter traceSpanConverter;

    @Mock
    private IssueAnalyzer issueAnalyzer;

    @InjectMocks
    private TryIssuesService tryIssuesService;

    private String tryId;
    private String traceId;

    @BeforeEach
    void setUp() {
        tryId = "test-try-id";
        traceId = "test-trace-id";
    }

    @Test
    @DisplayName("Tempo가 비활성화된 경우 빈 응답 반환")
    void getIssues_TempoDisabled_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(false);

        // when
        TryIssuesResponse response = tryIssuesService.getIssues(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertTrue(response.getIssues().isEmpty());

        verify(tempoClient, never()).pollForTrace(anyString());
    }

    @Test
    @DisplayName("Trace를 찾지 못한 경우 빈 응답 반환")
    void getIssues_TraceNotFound_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(null);

        // when
        TryIssuesResponse response = tryIssuesService.getIssues(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertTrue(response.getIssues().isEmpty());
    }

    @Test
    @DisplayName("Trace 데이터가 null인 경우 빈 응답 반환")
    void getIssues_TraceDataNull_ReturnsEmptyResponse() throws Exception {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenReturn(traceId);
        when(tempoClient.getTrace(traceId)).thenReturn(null);

        // when
        TryIssuesResponse response = tryIssuesService.getIssues(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertTrue(response.getIssues().isEmpty());
    }

    @Test
    @DisplayName("정상적으로 issues 조회 성공")
    void getIssues_Success() throws Exception {
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
        TryIssuesResponse response = tryIssuesService.getIssues(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertEquals(1, response.getIssues().size());
        assertEquals(Issue.Type.SLOW_SPAN, response.getIssues().get(0).getType());
        assertEquals(Issue.Severity.HIGH, response.getIssues().get(0).getSeverity());

        verify(tempoClient).pollForTrace(contains(tryId));
        verify(traceSpanConverter).convert(traceData);
        verify(issueAnalyzer).analyze(spans, 100L);
    }

    @Test
    @DisplayName("예외 발생 시 빈 응답 반환")
    void getIssues_Exception_ReturnsEmptyResponse() {
        // given
        when(tempoClient.isEnabled()).thenReturn(true);
        when(tempoClient.pollForTrace(anyString())).thenThrow(new RuntimeException("Test exception"));

        // when
        TryIssuesResponse response = tryIssuesService.getIssues(tryId);

        // then
        assertNotNull(response);
        assertEquals(tryId, response.getTryId());
        assertTrue(response.getIssues().isEmpty());
    }

    private List<TraceSpanInfo> createTestSpans() {
        // calculateTotalDuration은 (maxEnd - minStart) / 1_000_000으로 계산하므로
        // 100ms를 얻으려면 100_000_000 나노초 차이가 필요함
        TraceSpanInfo span1 = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("OrderController.getOrder")
                .kind("SERVER")
                .startTimeNanos(0L)
                .endTimeNanos(100_000_000L) // 100ms in nanoseconds
                .durationNanos(100_000_000L)
                .durationMs(100L)
                .attributes(new HashMap<>())
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
