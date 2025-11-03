package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client;

import java.util.List;

/**
 * Tempo REST API client for querying traces.
 * <p>
 * This interface provides methods for interacting with Tempo (distributed tracing backend)
 * to query and retrieve trace data.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 *   <li>Query traces by TraceQL</li>
 *   <li>Poll for trace availability with timeout</li>
 *   <li>Fetch trace data by trace ID</li>
 *   <li>Check if Tempo is enabled and available</li>
 * </ul>
 * <p>
 * Implementation: {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client.RestTemplateTempoClient}
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public interface TempoClient {
    
    /**
     * Searches for traces using TraceQL query.
     * <p>
     * Executes a TraceQL query against Tempo and returns matching trace IDs.
     *
     * @param query TraceQL query string (e.g., "{ span.ouro.try_id = \"tryId\" }")
     * @return List of trace IDs matching the query, empty list if none found
     */
    List<String> searchTraces(String query);
    
    /**
     * Fetches trace data by trace ID.
     * <p>
     * Retrieves full trace data from Tempo for the specified trace ID.
     * The returned data format depends on Tempo API (typically JSON).
     *
     * @param traceId the trace ID to fetch
     * @return Trace data in JSON format, or null if not found
     */
    String getTrace(String traceId);
    
    /**
     * Polls for traces matching the query until found or timeout.
     * <p>
     * Repeatedly searches for traces matching the query with a polling interval
     * until a trace is found or timeout is reached. Useful for waiting for
     * traces that may not be immediately available in Tempo.
     *
     * @param query TraceQL query string (e.g., "{ span.ouro.try_id = \"tryId\" }")
     * @return Trace ID if found, null if timeout
     */
    String pollForTrace(String query);
    
    /**
     * Checks if Tempo is enabled and available.
     * <p>
     * Determines whether Tempo integration is configured and available.
     * Returns false if Tempo is disabled or not configured.
     *
     * @return true if Tempo is enabled and available, false otherwise
     */
    boolean isEnabled();
}

