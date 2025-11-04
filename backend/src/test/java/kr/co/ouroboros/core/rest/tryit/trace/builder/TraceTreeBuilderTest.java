package kr.co.ouroboros.core.rest.tryit.trace.builder;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanMethodInfo;
import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import kr.co.ouroboros.core.rest.tryit.trace.parser.SpanMethodParser;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceTreeBuilder 테스트")
class TraceTreeBuilderTest {

    @Mock
    private SpanMethodParser spanMethodParser;

    @InjectMocks
    private TraceTreeBuilder traceTreeBuilder;

    @BeforeEach
    void setUp() {
        // 기본 SpanMethodInfo는 개별 테스트에서 필요할 때만 설정
    }

    @Test
    @DisplayName("빈 spans 리스트는 빈 트리 반환")
    void buildTree_EmptySpans_ReturnsEmptyTree() {
        // given
        List<TraceSpanInfo> spans = new ArrayList<>();

        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(spans, 100L);

        // then
        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("null spans 리스트는 빈 트리 반환")
    void buildTree_NullSpans_ReturnsEmptyTree() {
        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(null, 100L);

        // then
        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("단일 root span으로 트리 구성")
    void buildTree_SingleRootSpan_BuildsTree() {
        // given
        TraceSpanInfo rootSpan = createSpan("span1", null, "RootClass.rootMethod", 100L);
        
        SpanMethodInfo methodInfo = SpanMethodInfo.builder()
                .className("RootClass")
                .methodName("rootMethod")
                .parameters(new ArrayList<>())
                .build();
        when(spanMethodParser.parse(rootSpan)).thenReturn(methodInfo);

        List<TraceSpanInfo> spans = List.of(rootSpan);

        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(spans, 100L);

        // then
        assertNotNull(tree);
        assertEquals(1, tree.size());
        assertEquals("span1", tree.get(0).getSpanId());
        assertEquals("RootClass.rootMethod", tree.get(0).getName());
        assertEquals("RootClass", tree.get(0).getClassName());
        assertEquals("rootMethod", tree.get(0).getMethodName());
        
        verify(spanMethodParser).parse(rootSpan);
    }

    @Test
    @DisplayName("부모-자식 관계의 spans로 트리 구성")
    void buildTree_ParentChildSpans_BuildsHierarchicalTree() {
        // given
        TraceSpanInfo rootSpan = createSpan("span1", null, "RootClass.rootMethod", 100L);
        TraceSpanInfo childSpan = createSpan("span2", "span1", "ChildClass.childMethod", 50L);

        SpanMethodInfo rootMethodInfo = SpanMethodInfo.builder()
                .className("RootClass")
                .methodName("rootMethod")
                .parameters(new ArrayList<>())
                .build();
        SpanMethodInfo childMethodInfo = SpanMethodInfo.builder()
                .className("ChildClass")
                .methodName("childMethod")
                .parameters(new ArrayList<>())
                .build();

        when(spanMethodParser.parse(rootSpan)).thenReturn(rootMethodInfo);
        when(spanMethodParser.parse(childSpan)).thenReturn(childMethodInfo);

        List<TraceSpanInfo> spans = List.of(rootSpan, childSpan);

        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(spans, 100L);

        // then
        assertNotNull(tree);
        assertEquals(1, tree.size());
        SpanNode root = tree.get(0);
        assertEquals("span1", root.getSpanId());
        assertNotNull(root.getChildren());
        assertEquals(1, root.getChildren().size());
        assertEquals("span2", root.getChildren().get(0).getSpanId());
        
        verify(spanMethodParser).parse(rootSpan);
        verify(spanMethodParser).parse(childSpan);
    }

    @Test
    @DisplayName("self duration 계산 테스트")
    void buildTree_SelfDurationCalculation() {
        // given
        TraceSpanInfo rootSpan = createSpan("span1", null, "RootClass.rootMethod", 100L);
        TraceSpanInfo childSpan = createSpan("span2", "span1", "ChildClass.childMethod", 50L);

        SpanMethodInfo rootMethodInfo = SpanMethodInfo.builder()
                .className("RootClass")
                .methodName("rootMethod")
                .parameters(new ArrayList<>())
                .build();
        SpanMethodInfo childMethodInfo = SpanMethodInfo.builder()
                .className("ChildClass")
                .methodName("childMethod")
                .parameters(new ArrayList<>())
                .build();

        when(spanMethodParser.parse(rootSpan)).thenReturn(rootMethodInfo);
        when(spanMethodParser.parse(childSpan)).thenReturn(childMethodInfo);

        List<TraceSpanInfo> spans = List.of(rootSpan, childSpan);

        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(spans, 100L);

        // then
        SpanNode root = tree.get(0);
        assertEquals(100L, root.getDurationMs());
        assertEquals(50L, root.getSelfDurationMs()); // 100 - 50 = 50
        assertEquals(50L, root.getChildren().get(0).getDurationMs());
        assertEquals(50L, root.getChildren().get(0).getSelfDurationMs());
        
        verify(spanMethodParser).parse(rootSpan);
        verify(spanMethodParser).parse(childSpan);
    }

    @Test
    @DisplayName("HTTP span의 name 포맷팅 테스트")
    void buildTree_HttpSpan_FormatsDisplayName() {
        // given
        Map<String, String> attributes = new HashMap<>();
        attributes.put("method", "GET");
        attributes.put("http.url", "http://example.com/api/users/123");

        TraceSpanInfo httpSpan = TraceSpanInfo.builder()
                .spanId("span1")
                .parentSpanId(null)
                .name("http get /api/users/{id}")
                .kind("SERVER")
                .startTimeNanos(1000L)
                .endTimeNanos(1100L)
                .durationNanos(100L)
                .durationMs(100L)
                .attributes(attributes)
                .build();

        SpanMethodInfo httpMethodInfo = SpanMethodInfo.builder()
                .className("HTTP")
                .methodName("http get /api/users/{id}")
                .parameters(new ArrayList<>())
                .build();
        when(spanMethodParser.parse(httpSpan)).thenReturn(httpMethodInfo);

        List<TraceSpanInfo> spans = List.of(httpSpan);

        // when
        List<SpanNode> tree = traceTreeBuilder.buildTree(spans, 100L);

        // then
        SpanNode node = tree.get(0);
        assertNotNull(node.getName());
        assertTrue(node.getName().startsWith("http "));
        assertEquals("HTTP", node.getClassName());
        
        verify(spanMethodParser).parse(httpSpan);
    }

    private TraceSpanInfo createSpan(String spanId, String parentSpanId, String name, Long durationMs) {
        long startTime = 1000L;
        long durationNanos = durationMs * 1_000_000;
        long endTime = startTime + durationNanos;

        return TraceSpanInfo.builder()
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(name)
                .kind("INTERNAL")
                .startTimeNanos(startTime)
                .endTimeNanos(endTime)
                .durationNanos(durationNanos)
                .durationMs(durationMs)
                .attributes(new HashMap<>())
                .build();
    }
}
