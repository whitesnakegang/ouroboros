/**
 * Storage components for Try feature.
 * <p>
 * This package contains storage-related components for storing and retrieving
 * trace data from various storage backends (Tempo, in-memory, database, etc.).
 * <p>
 * <b>Interfaces:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceClient} - Common interface for trace querying</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage} - Common interface for trace storage</li>
 * </ul>
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceDataRetriever} - Component for retrieving and parsing trace data</li>
 * </ul>
 * <p>
 * <b>Subpackages:</b>
 * <ul>
 *   <li><b>model</b> - Common trace data models (TraceDTO)</li>
 *   <li><b>memory</b> - In-memory storage implementation</li>
 *   <li><b>tempo</b> - Tempo (distributed tracing backend) integration</li>
 * </ul>
 * <p>
 * <b>Design:</b>
 * <p>
 * This package follows a pluggable architecture where different storage backends
 * can be implemented by implementing the {@code TraceClient} and {@code TraceStorage}
 * interfaces. The implementation is selected based on configuration properties.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.storage;

