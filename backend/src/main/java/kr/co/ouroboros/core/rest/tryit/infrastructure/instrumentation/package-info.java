/**
 * Instrumentation components for Try feature.
 * <p>
 * This package contains instrumentation components for automatic
 * method-level tracing using AOP and OpenTelemetry.
 * <p>
 * <b>Subpackages:</b>
 * <ul>
 *   <li><b>aspect</b> - AOP interceptors for method tracing</li>
 *   <li><b>context</b> - Try context management using OpenTelemetry Baggage</li>
 *   <li><b>processor</b> - OpenTelemetry span processors</li>
 * </ul>
 * <p>
 * <b>Note:</b> Configuration classes have been moved to {@link kr.co.ouroboros.core.rest.tryit.config}.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation;
