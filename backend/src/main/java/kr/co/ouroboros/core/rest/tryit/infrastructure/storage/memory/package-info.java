/**
 * In-memory trace storage components.
 * <p>
 * This package contains components for storing and retrieving traces in memory
 * when Tempo is disabled. Provides a fallback storage mechanism that is
 * compatible with the existing TraceClient interface.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li><b>InMemoryTraceStorage</b> - Thread-safe in-memory storage for traces</li>
 *   <li><b>processor</b> - OpenTelemetry SpanProcessor for in-memory storage</li>
 * </ul>
 * <p>
 * <b>Subpackages:</b>
 * <ul>
 *   <li><b>client</b> - TraceClient implementations for in-memory storage</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <p>
 * When {@code ouroboros.tempo.enabled=false} (or not set), these components are
 * automatically used instead of Tempo server integration. Spans are collected
 * in memory and can be retrieved using the same TraceClient interface.
 * <p>
 * <b>Configuration:</b>
 * <pre>{@code
 * # Disable Tempo to use in-memory storage
 * ouroboros.tempo.enabled=false
 * }</pre>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory;

