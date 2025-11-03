package kr.co.ouroboros.core.rest.tryit.trace.analyzer;

import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IssueAnalyzer 테스트")
class IssueAnalyzerTest {

    private IssueAnalyzer issueAnalyzer;

    @BeforeEach
    void setUp() {
        issueAnalyzer = new IssueAnalyzer();
    }

    @Test
    @DisplayName("빈 spans 리스트는 빈 issues 반환")
    void analyze_EmptySpans_ReturnsEmptyIssues() {
        // given
        List<TraceSpanInfo> spans = new ArrayList<>();
        long totalDurationMs = 100L;

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    @DisplayName("null spans 리스트는 빈 issues 반환")
    void analyze_NullSpans_ReturnsEmptyIssues() {
        // given
        long totalDurationMs = 100L;

        // when
        List<Issue> issues = issueAnalyzer.analyze(null, totalDurationMs);

        // then
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    @DisplayName("느린 DB 쿼리 감지 테스트")
    void analyze_SlowDbQuery_DetectsIssue() {
        // given
        TraceSpanInfo dbSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("repository.query")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(600_000_000L) // 600ms
                .durationNanos(600_000_000L)
                .durationMs(600L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(dbSpan);
        long totalDurationMs = 1000L; // 60% of total time

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.stream()
                .filter(i -> i.getType() == Issue.Type.DB_QUERY_SLOW)
                .findFirst()
                .orElse(null);
        assertNotNull(issue);
        assertEquals(Issue.Severity.HIGH, issue.getSeverity());
        assertTrue(issue.getSummary().contains("DB query"));
        assertTrue(issue.getSummary().contains("60"));
    }

    @Test
    @DisplayName("느린 HTTP 호출 감지 테스트")
    void analyze_SlowHttpCall_DetectsIssue() {
        // given
        TraceSpanInfo httpSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("http get /api/users")
                .kind("CLIENT")
                .startTimeNanos(0L)
                .endTimeNanos(400_000_000L) // 400ms
                .durationNanos(400_000_000L)
                .durationMs(400L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(httpSpan);
        long totalDurationMs = 1000L; // 40% of total time

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.stream()
                .filter(i -> i.getType() == Issue.Type.SLOW_HTTP)
                .findFirst()
                .orElse(null);
        assertNotNull(issue);
        assertEquals(Issue.Severity.MEDIUM, issue.getSeverity());
        assertTrue(issue.getSummary().contains("HTTP call"));
    }

    @Test
    @DisplayName("일반적으로 느린 span 감지 테스트")
    void analyze_SlowSpan_DetectsIssue() {
        // given
        TraceSpanInfo slowSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("SlowClass.slowMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(300_000_000L) // 300ms
                .durationNanos(300_000_000L)
                .durationMs(300L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(slowSpan);
        long totalDurationMs = 1000L; // 30% of total time (> 20%)

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.stream()
                .filter(i -> i.getType() == Issue.Type.SLOW_SPAN)
                .findFirst()
                .orElse(null);
        assertNotNull(issue);
        assertTrue(issue.getSummary().contains("Span takes"));
    }

    @Test
    @DisplayName("심각도 결정 테스트 - CRITICAL")
    void analyze_CriticalSeverity() {
        // given
        TraceSpanInfo criticalSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("CriticalClass.criticalMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(800_000_000L) // 800ms
                .durationNanos(800_000_000L)
                .durationMs(800L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(criticalSpan);
        long totalDurationMs = 1000L; // 80% of total time (>= 75%)

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.get(0);
        assertEquals(Issue.Severity.CRITICAL, issue.getSeverity());
    }

    @Test
    @DisplayName("심각도 결정 테스트 - HIGH")
    void analyze_HighSeverity() {
        // given
        TraceSpanInfo highSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("HighClass.highMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(600_000_000L) // 600ms
                .durationNanos(600_000_000L)
                .durationMs(600L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(highSpan);
        long totalDurationMs = 1000L; // 60% of total time (>= 50%, < 75%)

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.get(0);
        assertEquals(Issue.Severity.HIGH, issue.getSeverity());
    }

    @Test
    @DisplayName("심각도 결정 테스트 - MEDIUM")
    void analyze_MediumSeverity() {
        // given
        TraceSpanInfo mediumSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("MediumClass.mediumMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(300_000_000L) // 300ms
                .durationNanos(300_000_000L)
                .durationMs(300L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(mediumSpan);
        long totalDurationMs = 1000L; // 30% of total time (>= 25%, < 50%)

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.get(0);
        assertEquals(Issue.Severity.MEDIUM, issue.getSeverity());
    }

    @Test
    @DisplayName("심각도 결정 테스트 - LOW")
    void analyze_LowSeverity() {
        // given
        TraceSpanInfo lowSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("LowClass.lowMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(220_000_000L) // 220ms
                .durationNanos(220_000_000L)
                .durationMs(220L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(lowSpan);
        long totalDurationMs = 1000L; // 22% of total time (>= 20% so detected, but < 25% so severity is LOW)

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.get(0);
        // 22% is >= 20% so detected, and < 25% so severity should be LOW
        assertEquals(Issue.Severity.LOW, issue.getSeverity());
    }

    @Test
    @DisplayName("증거(evidence) 생성 테스트")
    void analyze_GeneratesEvidence() {
        // given
        TraceSpanInfo span = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("TestClass.testMethod")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(300_000_000L)
                .durationNanos(300_000_000L)
                .durationMs(300L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(span);
        long totalDurationMs = 1000L;

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.get(0);
        assertNotNull(issue.getEvidence());
        assertFalse(issue.getEvidence().isEmpty());
        assertTrue(issue.getEvidence().stream().anyMatch(e -> e.contains("duration")));
        assertTrue(issue.getEvidence().stream().anyMatch(e -> e.contains("kind")));
        assertTrue(issue.getEvidence().stream().anyMatch(e -> e.contains("name")));
    }

    @Test
    @DisplayName("권장사항(recommendation) 포함 테스트")
    void analyze_IncludesRecommendation() {
        // given
        TraceSpanInfo span = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("repository.query")
                .kind("INTERNAL")
                .startTimeNanos(0L)
                .endTimeNanos(600_000_000L)
                .durationNanos(600_000_000L)
                .durationMs(600L)
                .attributes(new HashMap<>())
                .build();

        List<TraceSpanInfo> spans = List.of(span);
        long totalDurationMs = 1000L;

        // when
        List<Issue> issues = issueAnalyzer.analyze(spans, totalDurationMs);

        // then
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        Issue issue = issues.stream()
                .filter(i -> i.getType() == Issue.Type.DB_QUERY_SLOW)
                .findFirst()
                .orElse(null);
        assertNotNull(issue);
        assertNotNull(issue.getRecommendation());
        assertFalse(issue.getRecommendation().isEmpty());
        assertTrue(issue.getRecommendation().contains("index") || issue.getRecommendation().contains("optimization"));
    }
}
