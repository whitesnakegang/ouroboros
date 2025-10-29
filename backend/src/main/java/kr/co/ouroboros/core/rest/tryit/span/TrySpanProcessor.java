package kr.co.ouroboros.core.rest.tryit.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import kr.co.ouroboros.core.rest.tryit.util.TryContext;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenTelemetry SpanProcessor that automatically adds tryId attribute to spans
 * when a Try request is active.
 * 
 * This processor runs on span start and adds the tryId attribute if available
 * in the current context.
 */
@Slf4j
public class TrySpanProcessor implements SpanProcessor {
    
    private static final AttributeKey<String> TRY_ID_ATTRIBUTE = AttributeKey.stringKey("ouro.try_id");
    
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // Check if there's a tryId in the current context
        String tryId = TryContext.getTryIdFromBaggage();
        if (tryId != null) {
            span.setAttribute(TRY_ID_ATTRIBUTE, tryId);
            log.debug("Added tryId attribute to span: {}", tryId);
        }
    }
    
    @Override
    public boolean isStartRequired() {
        return true;
    }
    
    @Override
    public void onEnd(ReadableSpan span) {
        // No action needed on span end
    }
    
    @Override
    public boolean isEndRequired() {
        return false;
    }
}
