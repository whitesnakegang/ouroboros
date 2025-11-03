package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.processor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * OpenTelemetry SpanProcessor that automatically adds tryId attribute to spans.
 * <p>
 * This processor runs on span start and adds the tryId attribute from
 * OpenTelemetry Baggage (set by {@link kr.co.ouroboros.core.rest.tryit.identification.TryFilter})
 * to all spans created during Try request processing.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Runs on span start (onStart)</li>
 *   <li>Adds "ouro.try_id" attribute to spans when Try request is active</li>
 *   <li>Reads tryId from TryContext (which uses OpenTelemetry Baggage)</li>
 *   <li>No action on span end</li>
 * </ul>
 * <p>
 * This processor is automatically registered by Spring Boot's Micrometer Tracing
 * auto-configuration when configured in {@link kr.co.ouroboros.core.rest.tryit.config.TryConfig}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public class TrySpanProcessor implements SpanProcessor {
    
    private static final AttributeKey<String> TRY_ID_ATTRIBUTE = AttributeKey.stringKey("ouro.try_id");
    
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

