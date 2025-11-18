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
import org.springframework.web.bind.annotation.DeleteMapping;
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
 *   <li>DELETE /ouro/tries/{tryId} - Deletes trace data for the given tryId</li>
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
     * Retrieves summary metadata for the given Try session.
     *
     * The summary includes tryId, traceId, analysis status (PENDING, COMPLETED, FAILED),
     * HTTP status code, total duration in milliseconds, span count, and issue count.
     *
     * @param tryIdStr Try session ID; must be a valid UUID
     * @return GlobalApiResponse containing a TrySummaryResponse with the requested summary metadata
     * @throws InvalidTryIdException if tryIdStr is not a valid UUID
     * @throws Exception if retrieval of the summary fails
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
     * Retrieve a paginated list of methods for a Try ordered by self-duration (descending).
     *
     * @param tryIdStr Try session ID as a UUID string
     * @param page     Page index (must be greater than or equal to 0)
     * @param size     Page size (must be between 1 and 100)
     * @return         a GlobalApiResponse containing a TryMethodListResponse with method entries and pagination metadata
     * @throws kr.co.ouroboros.core.rest.tryit.exception.InvalidTryIdException if `tryIdStr` is not a valid UUID
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
     * Retrieve the hierarchical call trace for a Try session optimized for visualization.
     *
     * <p>The response contains hierarchical spans with parent-child relationships and total durations; issue detection is omitted for performance.</p>
     *
     * @param tryIdStr Try session ID as a UUID string
     * @return a TryTraceResponse containing hierarchical spans wrapped in a GlobalApiResponse
     * @throws InvalidTryIdException if tryIdStr is not a valid UUID
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
     * Retrieve detected issues and recommendations for a Try session without including trace spans.
     *
     * Identifies performance bottlenecks such as N+1 queries, slow HTTP calls, and slow database queries,
     * and provides severity and remediation recommendations.
     *
     * @param tryIdStr the Try session ID; must be a valid UUID
     * @return a GlobalApiResponse containing detected issues and recommendations for the specified Try
     * @throws InvalidTryIdException if the provided tryIdStr is not a valid UUID
     * @throws Exception if retrieval of issues fails
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
     * Deletes trace data for the given tryId from trace storage.
     * <p>
     * This endpoint removes the trace data stored in the trace storage (e.g., in-memory storage)
     * for the specified tryId. This is useful for cleaning up trace data to prevent memory leaks
     * when using in-memory storage.
     *
     * @param tryIdStr the Try session ID; must be a valid UUID
     * @return a GlobalApiResponse indicating whether the trace was successfully deleted
     * @throws InvalidTryIdException if the provided tryIdStr is not a valid UUID
     */
    @DeleteMapping("/{tryId}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteTrace(
            @PathVariable("tryId") String tryIdStr) {
        // Validate tryId format
        validateTryId(tryIdStr);
        
        boolean deleted = tryTraceService.deleteTrace(tryIdStr);
        
        if (deleted) {
            GlobalApiResponse<Void> response = GlobalApiResponse.success(
                    null,
                    "Trace deleted successfully for tryId: " + tryIdStr
            );
            return ResponseEntity.ok(response);
        } else {
            GlobalApiResponse<Void> response = GlobalApiResponse.success(
                    null,
                    "Trace not found for tryId: " + tryIdStr
            );
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Validate that the given tryId string is a valid UUID.
     *
     * @param tryIdStr the try identifier string to validate as a UUID
     * @throws InvalidTryIdException if {@code tryIdStr} is not a valid UUID
     */
    private void validateTryId(String tryIdStr) {
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidTryIdException(tryIdStr);
        }
    }
}
