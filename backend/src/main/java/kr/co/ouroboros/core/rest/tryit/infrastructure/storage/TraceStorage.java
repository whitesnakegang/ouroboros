package kr.co.ouroboros.core.rest.tryit.infrastructure.storage;

import io.opentelemetry.sdk.trace.ReadableSpan;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO;

/**
 * Common interface for trace storage backends.
 * <p>
 * This interface provides a unified API for storing and retrieving trace data
 * from various storage backends (in-memory, database, etc.).
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Store spans for a given tryId</li>
 *   <li>Retrieve trace data by tryId or traceId</li>
 *   <li>Check if a trace exists</li>
 * </ul>
 * <p>
 * <b>Implementations:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.InMemoryTraceStorage} - In-memory storage</li>
 *   <li>Future: DatabaseTraceStorage - Database storage</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public interface TraceStorage {
    
    /**
     * Adds a span to the storage for the given tryId.
     * <p>
     * The span should have a tryId attribute. If the span doesn't have a tryId,
     * it may be ignored depending on the implementation.
     *
     * @param span The span to store
     */
    void addSpan(ReadableSpan span);
    
    /**
     * Retrieves trace data by tryId.
     *
     * @param tryId The try ID to look up
     * @return TraceDTO if found, null otherwise
     */
    TraceDTO getTraceByTryId(String tryId);
    
    /**
     * Retrieves trace data by traceId.
     *
     * @param traceId The trace ID to look up
     * @return TraceDTO if found, null otherwise
     */
    TraceDTO getTraceByTraceId(String traceId);
    
    /**
     * Checks if a trace exists for the given tryId.
     *
     * @param tryId The try ID to check
     * @return true if trace exists, false otherwise
     */
    boolean hasTrace(String tryId);
    
    /**
     * Gets the trace ID for a given tryId.
     *
     * @param tryId The try ID to look up
     * @return Trace ID if found, null otherwise
     */
    String getTraceId(String tryId);
    
    /**
     * Deletes trace data for the given tryId.
     *
     * @param tryId The try ID to delete
     * @return true if trace was found and deleted, false otherwise
     */
    boolean deleteTraceByTryId(String tryId);
}

