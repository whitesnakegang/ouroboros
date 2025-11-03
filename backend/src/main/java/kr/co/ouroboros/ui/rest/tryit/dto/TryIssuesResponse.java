package kr.co.ouroboros.ui.rest.tryit.dto;

import kr.co.ouroboros.core.rest.tryit.trace.dto.Issue;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for Try issues retrieval API.
 * <p>
 * Response for GET /ouro/tries/{tryId}/issues
 * <p>
 * Contains only detected issues without trace spans.
 * Optimized for issues analysis and recommendations.
 *
 * @author Ouroboros Team
 * @since 0.0.1
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

