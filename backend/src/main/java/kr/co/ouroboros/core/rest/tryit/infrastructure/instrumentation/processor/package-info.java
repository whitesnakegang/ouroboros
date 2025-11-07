/**
 * OpenTelemetry span processor base components.
 * <p>
 * This package contains the abstract base class for Try-related SpanProcessors
 * that automatically add tryId attributes to spans during Try request processing.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor.AbstractTrySpanProcessor} - Abstract base class providing common tryId attribute logic</li>
 * </ul>
 * <p>
 * <b>Note:</b> Concrete SpanProcessor implementations are located in their respective
 * storage packages:
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.processor.TempoTrySpanProcessor} - For Tempo-enabled environments</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.processor.InMemoryTrySpanProcessor} - For in-memory storage</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor;

