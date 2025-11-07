/**
 * Tempo (distributed tracing backend) integration components.
 * <p>
 * This package contains components for integrating with Tempo,
 * a distributed tracing backend, including clients and processors.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li><b>client</b> - Tempo REST API client implementations</li>
 *   <li><b>processor</b> - OpenTelemetry SpanProcessor for Tempo-enabled environments</li>
 * </ul>
 * <p>
 * <b>Notes:</b>
 * <ul>
 *   <li>Common trace data models (TraceDTO) are now in
 *       {@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model} package.</li>
 *   <li>Configuration properties have been moved to
 *       {@link kr.co.ouroboros.core.rest.tryit.config.properties} package.</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo;

