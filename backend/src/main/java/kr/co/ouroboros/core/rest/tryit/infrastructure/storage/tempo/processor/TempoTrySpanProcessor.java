package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.tempo.processor;

import io.opentelemetry.sdk.trace.ReadableSpan;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor.AbstractTrySpanProcessor;

/**
 * OpenTelemetry SpanProcessor for Tempo-enabled environments.
 * <p>
 * This processor adds tryId attributes to spans but does not collect them locally.
 * Spans are automatically exported to Tempo by OpenTelemetry's default SpanExporter.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Runs on span start to add tryId attribute</li>
 *   <li>No action on span end (spans are exported to Tempo automatically)</li>
 *   <li>Optimized for performance: isEndRequired returns false</li>
 * </ul>
 * <p>
 * This processor is automatically registered when Tempo is enabled
 * in {@link kr.co.ouroboros.core.rest.tryit.config.TryConfig}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public class TempoTrySpanProcessor extends AbstractTrySpanProcessor {
    
    /**
     * No-op handler invoked when a span ends.
     * <p>
     * Spans are automatically exported to Tempo by OpenTelemetry's default SpanExporter,
     * so no local collection is needed.
     *
     * @param span the span that has ended
     */
    @Override
    public void onEnd(ReadableSpan span) {
        // No action needed on span end - spans are exported to Tempo automatically
    }
    
    /**
     * Indicates whether this processor requires onEnd callbacks for spans.
     * <p>
     * Returns false for performance optimization since spans are exported to Tempo automatically.
     *
     * @return false - onEnd callbacks are not required
     */
    @Override
    public boolean isEndRequired() {
        return false;
    }
}

