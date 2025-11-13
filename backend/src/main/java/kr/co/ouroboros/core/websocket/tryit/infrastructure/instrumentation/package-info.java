/**
 * Instrumentation components for Try feature in STOMP.
 * <p>
 * This package contains instrumentation components for automatic
 * context propagation in STOMP channel executors using OpenTelemetry.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation.OtelContextTaskDecorator} - TaskDecorator for propagating OpenTelemetry Context across asynchronous tasks</li>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation.TryChannelExecutorCustomizer} - BeanPostProcessor for applying TaskDecorator to STOMP channel executors</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.websocket.tryit.infrastructure.instrumentation;

