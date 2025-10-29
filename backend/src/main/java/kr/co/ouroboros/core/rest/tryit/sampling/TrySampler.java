package kr.co.ouroboros.core.rest.tryit.sampling;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.ParentBasedSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Sampler that records traces only for Try requests.
 * 
 * This sampler checks if ouro.try_id exists in Baggage to decide sampling:
 * - If ouro.try_id exists: Record and sample (Try request)
 * - If ouro.try_id does not exist: Drop (Normal request)
 * 
 * This ensures that traces are only created for requests with X-Ouroboros-Try header.
 * 
 * The sampler is wrapped with ParentBasedSampler to ensure child spans are also sampled
 * when the parent span is sampled.
 */
@Slf4j
public class TrySampler implements Sampler {
    
    private static final String BAGGAGE_KEY = "ouro.try_id";
    
    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            java.util.List<LinkData> parentLinks) {
        
        // Check if this is a Try request by examining Baggage
        String tryId = getTryIdFromBaggage(parentContext);
        
        if (tryId != null && !tryId.isEmpty()) {
            // Try request: Record and sample
            log.debug("TrySampler: Sampling Try request (tryId={})", tryId);
            return SamplingResult.create(
                    SamplingDecision.RECORD_AND_SAMPLE
            );
        } else {
            // Normal request: Drop (don't record)
            log.debug("TrySampler: Dropping normal request");
            return SamplingResult.create(
                    SamplingDecision.DROP
            );
        }
    }
    
    /**
     * Extracts tryId from OpenTelemetry Baggage.
     * 
     * @param context OpenTelemetry context
     * @return tryId from Baggage, or null if not set
     */
    private String getTryIdFromBaggage(Context context) {
        try {
            Baggage baggage = Baggage.fromContext(context);
            
            if (baggage != null) {
                return baggage.getEntryValue(BAGGAGE_KEY);
            }
        } catch (Exception e) {
            log.trace("Failed to extract Baggage: {}", e.getMessage());
        }
        return null;
    }
    
    @Override
    public String getDescription() {
        return "TrySampler (records only Try requests)";
    }
    
    /**
     * Creates a ParentBasedSampler that wraps this TrySampler.
     * This ensures that child spans are automatically sampled when the parent span is sampled.
     * 
     * @return ParentBasedSampler wrapping this TrySampler
     */
    public static Sampler createParentBased() {
        TrySampler rootSampler = new TrySampler();
        return ParentBasedSampler.create(rootSampler);
    }
}

