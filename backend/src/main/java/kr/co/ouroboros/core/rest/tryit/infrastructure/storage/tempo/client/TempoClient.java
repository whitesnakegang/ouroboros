package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.client;

import java.util.List;

/**
 * Tempo REST API client for querying traces.
 * 
 * Responsibilities:
 * - Query traces by TraceQL
 * - Poll for trace availability
 * - Fetch trace data
 */
public interface TempoClient {
    
    /**
     * Search for traces using TraceQL.
     * 
     * @param query TraceQL query string
     * @return list of trace IDs matching the query
     */
    List<String> searchTraces(String query);
    
    /**
     * Fetches trace data by trace ID.
     * 
     * @param traceId the trace ID
     * @return trace data (format depends on Tempo API)
     */
    String getTrace(String traceId);
    
    /**
     * Polls for traces matching the query until found or timeout.
     * 
     * @param query TraceQL query string
     * @return trace ID if found, null if timeout
     */
    String pollForTrace(String query);
    
    /**
     * Checks if Tempo is enabled and available.
     * 
     * @return true if enabled
     */
    boolean isEnabled();
}

