package kr.co.ouroboros.core.rest.tryit.infrastructure.storage;

import java.util.List;

/**
 * Common interface for trace storage clients.
 * <p>
 * This interface provides a unified API for querying and retrieving trace data
 * from various storage backends (Tempo, in-memory, database, etc.).
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Query traces by tryId or TraceQL</li>
 *   <li>Poll for trace availability with timeout</li>
 *   <li>Fetch trace data by trace ID</li>
 *   <li>Check if the storage backend is enabled and available</li>
 * </ul>
 * <p>
 * <b>Implementations:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.InMemoryTraceClient} - In-memory storage</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.RestTemplateTempoClient} - Tempo backend</li>
 * </ul>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public interface TraceClient {
    
    /**
     * Finds trace IDs that match the given query.
     * <p>
     * The query format depends on the implementation:
     * <ul>
     *   <li>Tempo: TraceQL query (e.g., "{ span.ouro.try_id = \"tryId\" }")</li>
     *   <li>In-memory: tryId string</li>
     * </ul>
     *
     * @param query Query string (TraceQL or tryId depending on implementation)
     * @return a list of trace IDs that match the query, or an empty list if none are found
     */
    List<String> searchTraces(String query);
    
    /**
     * Retrieve the full trace data for the specified trace ID.
     * <p>
     * The returned payload format depends on the implementation:
     * <ul>
     *   <li>Tempo: JSON string from Tempo API</li>
     *   <li>In-memory: JSON string converted from TraceDTO</li>
     * </ul>
     *
     * @param traceId the trace ID to fetch
     * @return the trace data payload (typically JSON), or `null` if no trace was found
     */
    String getTrace(String traceId);
    
    /**
     * Polls for traces matching the query until a trace is found or a timeout is reached.
     * <p>
     * This method repeatedly calls {@link #searchTraces(String)} until a trace is found
     * or the maximum number of attempts is reached.
     *
     * @param query Query string (TraceQL or tryId depending on implementation)
     * @return the found trace ID, or null if no trace is found before timeout
     */
    String pollForTrace(String query);
    
    /**
     * Checks whether the storage backend is configured and available.
     *
     * @return `true` if the storage backend is configured and available, `false` otherwise
     */
    boolean isEnabled();
}

