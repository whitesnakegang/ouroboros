package kr.co.ouroboros.core.rest.tryit.web.dto;

import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for try issues retrieval API.
 * GET /ouro/tries/{tryId}/issues
 * 
 * Contains only detected issues without trace spans.
 */
@Data
@Builder
public class TryIssuesResponse {
    
    /**
     * Try session ID.
     */
    private String tryId;
    
    /**
     * Detected issues (bottlenecks).
     */
    private List<Issue> issues;
}

