/**
 * Data transfer objects (DTOs) for trace data.
 * <p>
 * This package contains DTOs representing trace data structures including
 * spans, nodes, issues, and analysis status.
 * <p>
 * <b>DTOs:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo} - Span information extracted from TraceDTO</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.trace.dto.SpanNode} - Hierarchical span node with children</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.trace.dto.SpanMethodInfo} - Parsed method information from span</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.trace.dto.Issue} - Detected performance issue</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.trace.dto.AnalysisStatus} - Analysis status enumeration</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.trace.dto;

