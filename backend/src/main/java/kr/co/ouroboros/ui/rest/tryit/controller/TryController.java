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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for Try result retrieval.
 * Retrieves Try analysis results for QA analysis.
 * <p>
 * All endpoints return standardized {@link GlobalApiResponse} format.
 * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
 * <p>
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
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     * 
     * GET /ouro/tries/{tryId}
     * 
     * @param tryIdStr Try session ID
     * @return summary response wrapped in GlobalApiResponse
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
     * Retrieves paginated list of methods for a try, sorted by selfDurationMs (descending).
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     * 
     * GET /ouro/tries/{tryId}/methods
     * 
     * @param tryIdStr Try session ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 5)
     * @return paginated method list wrapped in GlobalApiResponse
     * @throws InvalidTryIdException if tryId format is invalid
     * @throws Exception if retrieval fails
     */
    @GetMapping("/{tryId}/methods")
    public ResponseEntity<GlobalApiResponse<TryMethodListResponse>> getMethods(
            @PathVariable("tryId") String tryIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
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
     * Retrieves full call trace for a try without analysis issues.
     * Optimized for call trace visualization (toggle tree view).
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     *
     * GET /ouro/tries/{tryId}/trace
     *
     * @param tryIdStr Try session ID
     * @return trace response with hierarchical spans wrapped in GlobalApiResponse
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
     * Retrieves detected issues for a try without trace spans.
     * Optimized for issues analysis and recommendations.
     * <p>
     * Exceptions are handled by {@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler}.
     *
     * GET /ouro/tries/{tryId}/issues
     *
     * @param tryIdStr Try session ID
     * @return issues response wrapped in GlobalApiResponse
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
     * 
     * @param tryIdStr tryId string to validate
     * @throws InvalidTryIdException if tryId format is invalid
     */
    private void validateTryId(String tryIdStr) {
        try {
            UUID.fromString(tryIdStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidTryIdException(tryIdStr);
        }
    }
}

