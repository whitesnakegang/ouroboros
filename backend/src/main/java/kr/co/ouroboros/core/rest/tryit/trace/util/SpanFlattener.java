package kr.co.ouroboros.core.rest.tryit.trace.util;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for flattening hierarchical span tree into a flat list.
 * <p>
 * This component converts hierarchical span trees (parent-child relationships)
 * into a flat list for processing and sorting operations.
 * <p>
 * Uses depth-first search (DFS) to traverse the tree and collect all spans
 * in a single flat list while preserving order.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@Component
public class SpanFlattener {
    
    /**
     * Flattens a hierarchical span tree into a flat list using DFS.
     * <p>
     * Traverses the hierarchical tree structure starting from root spans,
     * collecting all spans (including nested children) into a single flat list.
     * <p>
     * Order: Root spans are processed first, followed by their children
     * recursively in depth-first order.
     *
     * @param spanTree List of root span nodes with hierarchical children
     * @return Flat list containing all spans from the tree
     */
    public List<SpanNode> flatten(List<SpanNode> spanTree) {
        if (spanTree == null || spanTree.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SpanNode> flatList = new ArrayList<>();
        
        for (SpanNode root : spanTree) {
            flattenRecursive(root, flatList);
        }
        
        log.debug("Flattened {} root spans into {} total spans", spanTree.size(), flatList.size());
        return flatList;
    }
    
    /**
     * Flatten a SpanNode subtree into a flat list in depth-first order.
     *
     * Adds the given node followed by all of its descendants to the provided list.
     *
     * @param node the root of the subtree to flatten; if null this method returns immediately
     * @param flatList the list to append nodes to; must be non-null
     */
    private void flattenRecursive(SpanNode node, List<SpanNode> flatList) {
        if (node == null) {
            return;
        }
        
        // Add current node
        flatList.add(node);
        
        // Process children recursively
        if (node.getChildren() != null) {
            for (SpanNode child : node.getChildren()) {
                flattenRecursive(child, flatList);
            }
        }
    }
}
