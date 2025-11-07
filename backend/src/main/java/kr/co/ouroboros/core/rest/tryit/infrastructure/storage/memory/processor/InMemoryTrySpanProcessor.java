package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.memory.processor;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadableSpan;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor.AbstractTrySpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.storage.TraceStorage;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenTelemetry SpanProcessor for in-memory trace storage when Tempo is disabled.
 * <p>
 * This processor adds tryId attributes to spans and collects them in memory storage
 * for later retrieval. It is used when Tempo is not available or disabled.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Runs on span start to add tryId attribute</li>
 *   <li>Runs on span end to collect spans in memory</li>
 *   <li>Only collects spans that have a tryId attribute</li>
 *   <li>Thread-safe span collection</li>
 * </ul>
 * <p>
 * This processor is automatically registered when Tempo is disabled
 * in {@link kr.co.ouroboros.core.rest.tryit.config.TryConfig}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public class InMemoryTrySpanProcessor extends AbstractTrySpanProcessor {
    
    private final TraceStorage traceStorage;
    
    /**
     * Creates a new InMemoryTrySpanProcessor with the given storage.
     *
     * @param traceStorage The trace storage to use (typically InMemoryTraceStorage)
     */
    public InMemoryTrySpanProcessor(TraceStorage traceStorage) {
        this.traceStorage = traceStorage;
    }
    
    /**
     * Collects the span in memory storage when it ends.
     * <p>
     * Only collects spans that have a tryId attribute.
     *
     * @param span The span that has ended
     */
    @Override
    public void onEnd(ReadableSpan span) {
        String tryId = span.getAttribute(AbstractTrySpanProcessor.TRY_ID_ATTRIBUTE);
        if (tryId == null) {
            return;
        }
        
        traceStorage.addSpan(span);
        log.debug("Collected span in memory: tryId={}, spanId={}", 
                  tryId, span.getSpanContext().getSpanId());
    }
    
    /**
     * Indicates that span end processing is required.
     *
     * @return true if onEnd should be called for ended spans
     */
    @Override
    public boolean isEndRequired() {
        return true;
    }
    
    /**
     * Shuts down the processor.
     *
     * @return CompletableResultCode indicating success
     */
    @Override
    public CompletableResultCode shutdown() {
        log.debug("Shutting down InMemoryTrySpanProcessor");
        return CompletableResultCode.ofSuccess();
    }
    
    /**
     * Forces a flush of any buffered spans.
     *
     * @return CompletableResultCode indicating success
     */
    @Override
    public CompletableResultCode forceFlush() {
        log.debug("Force flushing InMemoryTrySpanProcessor");
        return CompletableResultCode.ofSuccess();
    }
}

