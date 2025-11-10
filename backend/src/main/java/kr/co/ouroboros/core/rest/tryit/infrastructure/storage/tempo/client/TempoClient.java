package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client;

import java.util.List;

/**
 * Tempo REST API client for querying traces.
 * <p>
 * <b>Deprecated:</b> This interface is deprecated. Use {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceClient} instead.
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
 * <b>Migration:</b>
 * <p>
 * Replace {@code TempoClient} with {@code TraceClient} in your code:
 * <pre>{@code
 * // Old
 * private final TempoClient tempoClient;
 * 
 * // New
 * private final TraceClient traceClient;
 * }</pre>
 *
 * @author Ouroboros Team
 * @since 0.0.1
 * @deprecated Use {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceClient} instead
 */
@Deprecated
public interface TempoClient {
    
    /**
 * Finds trace IDs that match the given TraceQL query.
 *
 * @param query TraceQL query string (e.g., "{ span.ouro.try_id = \"tryId\" }")
 * @return a list of trace IDs that match the query, or an empty list if none are found
 */
    List<String> searchTraces(String query);
    
    /**
 * Retrieve the full trace data for the specified Tempo trace ID.
 *
 * The returned payload is produced by the Tempo API (typically JSON).
 *
 * @param traceId the Tempo trace ID to fetch
 * @return the trace data payload as returned by Tempo, or `null` if no trace was found
 */
    String getTrace(String traceId);
    
    /**
 * Polls Tempo for traces matching the TraceQL query until a trace is found or a timeout is reached.
 *
 * @param query TraceQL query string, e.g. "{ span.ouro.try_id = \"tryId\" }"
 * @return the found trace ID, or null if no trace is found before timeout
 */
    String pollForTrace(String query);
    
    /**
 * Checks whether Tempo integration is configured and reachable.
 *
 * @return `true` if Tempo integration is configured and reachable, `false` otherwise
 */
    boolean isEnabled();
}
