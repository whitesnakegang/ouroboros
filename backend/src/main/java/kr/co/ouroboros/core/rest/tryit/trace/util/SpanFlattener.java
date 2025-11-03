package kr.co.ouroboros.core.rest.tryit.trace.util;

import kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for flattening hierarchical span tree into a flat list.
 */
@Slf4j
@Component
public class SpanFlattener {
    
    /**
     * Flattens a hierarchical span tree into a flat list using DFS.
     * 
     * @param spanTree Root spans
     * @return Flat list of all spans
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
     * Recursively flattens a span node and its children using DFS.
     * 
     * @param node Current span node
     * @param flatList Accumulated flat list
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

