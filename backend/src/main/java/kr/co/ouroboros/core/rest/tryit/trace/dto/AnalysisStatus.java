package kr.co.ouroboros.core.rest.tryit.trace.dto;

/**
 * Analysis status for a Try.
 * <p>
 * Represents the current state of trace analysis for a Try session.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public enum AnalysisStatus {
    /**
     * Analysis is pending (trace data not yet available).
     */
    PENDING,
    
    /**
     * Analysis completed successfully.
     */
    COMPLETED,
    
    /**
     * Analysis failed due to an error.
     */
    FAILED,
    
    /**
     * Trace data not found.
     */
    NOT_FOUND
}

