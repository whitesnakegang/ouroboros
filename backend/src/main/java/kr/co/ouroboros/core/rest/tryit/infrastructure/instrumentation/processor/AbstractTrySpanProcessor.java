package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Abstract base class for Try-related SpanProcessors.
 * <p>
 * This class provides common functionality for adding tryId attributes to spans.
 * Subclasses should implement {@link #onEnd(io.opentelemetry.sdk.trace.ReadableSpan)}
 * to provide span end processing behavior.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public abstract class AbstractTrySpanProcessor implements SpanProcessor {
    
    protected static final AttributeKey<String> TRY_ID_ATTRIBUTE = AttributeKey.stringKey("ouro.try_id");
    
    /**
     * Adds the current Try request's tryId to the span as the `ouro.try_id` attribute
     * when a Try request is active.
     * <p>
     * This method is called for all spans when a Try request is active.
     *
     * @param parentContext The parent context
     * @param span The span to modify
     */
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        UUID tryId = TryContext.getTryId();
        
        // Early return if this is not a Try request
        if (tryId == null) {
            return;
        }
        
        span.setAttribute(TRY_ID_ATTRIBUTE, tryId.toString());
        log.debug("Added tryId attribute to span: {}", tryId);
    }
    
    /**
     * Indicates that span start processing is required.
     *
     * @return true if onStart should be called for new spans
     */
    @Override
    public boolean isStartRequired() {
        return true;
    }
}

