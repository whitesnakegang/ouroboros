package kr.co.ouroboros.ui.rest.tryit.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TryIssuesResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TryMethodListResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TrySummaryResponse;
import kr.co.ouroboros.ui.rest.tryit.dto.TryTraceResponse;
import kr.co.ouroboros.core.rest.tryit.exception.InvalidTryIdException;
import kr.co.ouroboros.core.rest.tryit.service.TryIssuesService;
import kr.co.ouroboros.core.rest.tryit.service.TryMethodListService;
import kr.co.ouroboros.core.rest.tryit.service.TrySummaryService;
import kr.co.ouroboros.core.rest.tryit.service.TryTraceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for Try result retrieval.
 * <p>
 * Provides endpoints for retrieving Try analysis results for QA analysis.
 * All endpoints return standardized {@link GlobalApiResponse} format.
 * <p>
 * <b>Endpoints:</b>
 * <ul>
 *   <li>GET /ouro/tries/{tryId} - Retrieves Try summary</li>
 *   <li>GET /ouro/tries/{tryId}/methods - Retrieves paginated method list</li>
 *   <li>GET /ouro/tries/{tryId}/trace - Retrieves full call trace</li>
 *   <li>GET /ouro/tries/{tryId}/issues - Retrieves detected issues</li>
 * </ul>
 * <p>
 * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
 * <p>
 * <b>Note:</b> Session creation is no longer needed. Try requests are identified
 * by X-Ouroboros-Try header and tryId is returned in response header.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/ouro/tries")
@RequiredArgsConstructor
@Validated
public class TryController {

    private final TryMethodListService tryMethodListService;
    private final TryTraceService tryTraceService;
    private final TryIssuesService tryIssuesService;
    private final TrySummaryService trySummaryService;
    
    /**
     * Retrieves Try summary without detailed trace or issues.
     * <p>
     * Contains only metadata, counts, and status information:
     * <ul>
     *   <li>tryId and traceId</li>
     *   <li>Analysis status (PENDING, COMPLETED, FAILED)</li>
     *   <li>HTTP status code</li>
     *   <li>Total duration in milliseconds</li>
     *   <li>Span count and issue count</li>
     * </ul>
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     * 
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @return Summary response wrapped in GlobalApiResponse
     * @throws InvalidTryIdException if tryId format is invalid
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{tryId}")
    public ResponseEntity<GlobalApiResponse<TrySummaryResponse>> getSummary(
            @PathVariable("tryId") String tryIdStr) throws Exception {
        // Validate tryId format
        validateTryId(tryIdStr);
        
        TrySummaryResponse data = trySummaryService.getSummary(tryIdStr);
        GlobalApiResponse<TrySummaryResponse> response = GlobalApiResponse.success(
                data,
                "Try summary retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves paginated list of methods for a Try, sorted by selfDurationMs (descending).
     * <p>
     * Returns a paginated list of methods with:
     * <ul>
     *   <li>Method information (name, class, parameters)</li>
     *   <li>Self-duration and percentage</li>
     *   <li>Pagination metadata (page, size, totalCount, hasMore)</li>
     * </ul>
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     * 
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @param page Page number (default: 0, must be non-negative)
     * @param size Page size (default: 5, must be between 1 and 100)
     * @return Paginated method list wrapped in GlobalApiResponse
     * @throws InvalidTryIdException if tryId format is invalid
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{tryId}/methods")
    public ResponseEntity<GlobalApiResponse<TryMethodListResponse>> getMethods(
            @PathVariable("tryId") String tryIdStr,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size
    ) throws Exception {
        // Validate tryId format
        validateTryId(tryIdStr);
        
        TryMethodListResponse data = tryMethodListService.getMethodList(tryIdStr, page, size);
        GlobalApiResponse<TryMethodListResponse> response = GlobalApiResponse.success(
                data,
                "Try method list retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves full call trace for a Try without analysis issues.
     * <p>
     * Optimized for call trace visualization (toggle tree view):
     * <ul>
     *   <li>Hierarchical span structure</li>
     *   <li>Parent-child relationships</li>
     *   <li>Total duration</li>
     *   <li>Skips issue detection for performance</li>
     * </ul>
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     *
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @return Trace response with hierarchical spans wrapped in GlobalApiResponse
     * @throws InvalidTryIdException if tryId format is invalid
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{tryId}/trace")
    public ResponseEntity<GlobalApiResponse<TryTraceResponse>> getTrace(
            @PathVariable("tryId") String tryIdStr) throws Exception {
        // Validate tryId format
        validateTryId(tryIdStr);

        TryTraceResponse data = tryTraceService.getTrace(tryIdStr);
        GlobalApiResponse<TryTraceResponse> response = GlobalApiResponse.success(
                data,
                "Try trace retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detected issues for a Try without trace spans.
     * <p>
     * Optimized for issues analysis and recommendations:
     * <ul>
     *   <li>Performance bottlenecks</li>
     *   <li>N+1 query problems</li>
     *   <li>Slow HTTP calls and database queries</li>
     *   <li>Issue severity and recommendations</li>
     * </ul>
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     *
     * @param tryIdStr Try session ID (must be a valid UUID)
     * @return Issues response wrapped in GlobalApiResponse
     * @throws InvalidTryIdException if tryId format is invalid
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{tryId}/issues")
    public ResponseEntity<GlobalApiResponse<TryIssuesResponse>> getIssues(
            @PathVariable("tryId") String tryIdStr) throws Exception {
        // Validate tryId format
        validateTryId(tryIdStr);

        TryIssuesResponse data = tryIssuesService.getIssues(tryIdStr);
        GlobalApiResponse<TryIssuesResponse> response = GlobalApiResponse.success(
                data,
                "Try issues retrieved successfully"
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validates tryId format (must be a valid UUID).
     * <p>
     * Checks if the tryId string is a valid UUID format.
     * Throws {@link InvalidTryIdException} if validation fails.
     *
     * @param tryIdStr tryId string to validate
     * @throws InvalidTryIdException if tryId format is invalid (not a valid UUID)
     */
    private void validateTryId(String tryIdStr) {
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidTryIdException(tryIdStr);
        }
    }
}

