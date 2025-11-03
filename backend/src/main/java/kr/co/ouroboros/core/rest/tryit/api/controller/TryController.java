package kr.co.ouroboros.core.rest.tryit.api.controller;

import kr.co.ouroboros.core.rest.tryit.api.dto.TryIssuesResponse;
import kr.co.ouroboros.core.rest.tryit.api.dto.TryMethodListResponse;
import kr.co.ouroboros.core.rest.tryit.api.dto.TrySummaryResponse;
import kr.co.ouroboros.core.rest.tryit.api.dto.TryTraceResponse;
import kr.co.ouroboros.core.rest.tryit.service.TryIssuesService;
import kr.co.ouroboros.core.rest.tryit.service.TryMethodListService;
import kr.co.ouroboros.core.rest.tryit.service.TrySummaryService;
import kr.co.ouroboros.core.rest.tryit.service.TryTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * REST API controller for Try result retrieval.
 * Retrieves Try analysis results for QA analysis.
 * 
 * Note: Session creation is no longer needed. Try requests are identified
 * by X-Ouroboros-Try: on header and tryId is returned in response header.
 */
@Slf4j
@RestController
@RequestMapping("/ouro/tries")
@RequiredArgsConstructor
public class TryController {

    private final TryMethodListService tryMethodListService;
    private final TryTraceService tryTraceService;
    private final TryIssuesService tryIssuesService;
    private final TrySummaryService trySummaryService;
    
    /**
     * Retrieves Try summary without detailed trace or issues.
     * Contains only metadata, counts, and status information.
     * 
     * GET /ouro/tries/{tryId}
     * 
     * @param tryIdStr Try session ID
     * @return summary response
     */
    @GetMapping("/{tryId}")
    public TrySummaryResponse getSummary(@PathVariable("tryId") String tryIdStr) {
        // Validate tryId format
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }
        
        return trySummaryService.getSummary(tryIdStr);
    }
    
    /**
     * Retrieves paginated list of methods for a try, sorted by selfDurationMs (descending).
     * 
     * GET /ouro/tries/{tryId}/methods
     * 
     * @param tryIdStr Try session ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 5)
     * @return paginated method list
     */
    @GetMapping("/{tryId}/methods")
    public TryMethodListResponse getMethods(
            @PathVariable("tryId") String tryIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        // Validate tryId format
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }
        
        return tryMethodListService.getMethodList(tryIdStr, page, size);
    }

    /**
     * Retrieves full call trace for a try without analysis issues.
     * Optimized for call trace visualization (toggle tree view).
     *
     * GET /ouro/tries/{tryId}/trace
     *
     * @param tryIdStr Try session ID
     * @return trace response with hierarchical spans
     */
    @GetMapping("/{tryId}/trace")
    public TryTraceResponse getTrace(@PathVariable("tryId") String tryIdStr) {
        // Validate tryId format
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }

        return tryTraceService.getTrace(tryIdStr);
    }

    /**
     * Retrieves detected issues for a try without trace spans.
     * Optimized for issues analysis and recommendations.
     *
     * GET /ouro/tries/{tryId}/issues
     *
     * @param tryIdStr Try session ID
     * @return issues response
     */
    @GetMapping("/{tryId}/issues")
    public TryIssuesResponse getIssues(@PathVariable("tryId") String tryIdStr) {
        // Validate tryId format
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tryId format: {}", tryIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tryId format");
        }

        return tryIssuesService.getIssues(tryIdStr);
    }
}

