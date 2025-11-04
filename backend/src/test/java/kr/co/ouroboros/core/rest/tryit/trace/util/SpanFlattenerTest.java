package kr.co.ouroboros.core.rest.tryit.trace.util;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpanFlattener 테스트")
class SpanFlattenerTest {

    private SpanFlattener spanFlattener;

    @BeforeEach
    void setUp() {
        spanFlattener = new SpanFlattener();
    }

    @Test
    @DisplayName("빈 트리는 빈 리스트 반환")
    void flatten_EmptyTree_ReturnsEmptyList() {
        // given
        List<SpanNode> spanTree = new ArrayList<>();

        // when
        List<SpanNode> result = spanFlattener.flatten(spanTree);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("null 트리는 빈 리스트 반환")
    void flatten_NullTree_ReturnsEmptyList() {
        // when
        List<SpanNode> result = spanFlattener.flatten(null);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("단일 노드 트리 평탄화")
    void flatten_SingleNode_ReturnsSingleItem() {
        // given
        SpanNode node = createNode("span1", "Node1", null);
        List<SpanNode> spanTree = List.of(node);

        // when
        List<SpanNode> result = spanFlattener.flatten(spanTree);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("span1", result.get(0).getSpanId());
        assertEquals("Node1", result.get(0).getName());
    }

    @Test
    @DisplayName("부모-자식 관계 트리 평탄화")
    void flatten_ParentChildTree_FlattensCorrectly() {
        // given
        SpanNode child1 = createNode("span2", "Child1", null);
        SpanNode child2 = createNode("span3", "Child2", null);
        SpanNode root = createNode("span1", "Root", List.of(child1, child2));
        List<SpanNode> spanTree = List.of(root);

        // when
        List<SpanNode> result = spanFlattener.flatten(spanTree);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("span1", result.get(0).getSpanId()); // Root first (DFS)
        assertEquals("span2", result.get(1).getSpanId()); // Child1
        assertEquals("span3", result.get(2).getSpanId()); // Child2
    }

    @Test
    @DisplayName("다중 루트 노드 트리 평탄화")
    void flatten_MultipleRoots_FlattensAll() {
        // given
        SpanNode root1 = createNode("span1", "Root1", null);
        SpanNode root2 = createNode("span2", "Root2", null);
        List<SpanNode> spanTree = List.of(root1, root2);

        // when
        List<SpanNode> result = spanFlattener.flatten(spanTree);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("span1", result.get(0).getSpanId());
        assertEquals("span2", result.get(1).getSpanId());
    }

    @Test
    @DisplayName("깊은 계층 구조 평탄화")
    void flatten_DeepHierarchy_FlattensCorrectly() {
        // given
        SpanNode leaf = createNode("span4", "Leaf", null);
        SpanNode child = createNode("span3", "Child", List.of(leaf));
        SpanNode root = createNode("span1", "Root", List.of(child));
        List<SpanNode> spanTree = List.of(root);

        // when
        List<SpanNode> result = spanFlattener.flatten(spanTree);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("span1", result.get(0).getSpanId()); // Root
        assertEquals("span3", result.get(1).getSpanId()); // Child
        assertEquals("span4", result.get(2).getSpanId()); // Leaf
    }

    private SpanNode createNode(String spanId, String name, List<SpanNode> children) {
        return SpanNode.builder()
                .spanId(spanId)
                .name(name)
                .className("TestClass")
                .methodName("testMethod")
                .durationMs(100L)
                .selfDurationMs(100L)
                .percentage(100.0)
                .selfPercentage(100.0)
                .kind("INTERNAL")
                .children(children != null ? children : new ArrayList<>())
                .parameters(new ArrayList<>())
                .build();
    }
}
