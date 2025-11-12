package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.context.TryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * OpenTelemetry Sampler that only samples spans for Try requests.
 * <p>
 * This sampler checks if the current request has the X-Ouroboros-Try: on header
 * and only samples those spans. Non-Try requests are dropped, preventing span creation entirely.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>If X-Ouroboros-Try header equals "on" → RECORD_AND_SAMPLE (create span)</li>
 *   <li>If header is missing or not "on" → DROP (no span created, no Tempo export)</li>
 *   <li>Fallback: If no HTTP request context, checks TryContext.hasTryId() (for internal spans)</li>
 * </ul>
 * <p>
 * <b>Why check header directly instead of TryContext?</b>
 * <ul>
 *   <li>Sampler may execute BEFORE TryFilter sets the tryId in TryContext</li>
 *   <li>HTTP span creation happens during OpenTelemetry's ServerHttpObservationFilter</li>
 *   <li>Direct header check ensures accurate sampling decision at the earliest moment</li>
 * </ul>
 * <p>
 * <b>Why Sampler instead of SpanProcessor?</b>
 * <ul>
 *   <li>Sampler runs BEFORE span creation - can prevent span creation entirely</li>
 *   <li>SpanProcessor runs AFTER span creation - can only add attributes, not prevent creation</li>
 *   <li>This ensures non-Try requests generate zero tracing overhead and zero Tempo storage</li>
 * </ul>
 * <p>
 * This sampler is registered as a Spring Bean in {@link kr.co.ouroboros.core.rest.tryit.config.TraceStorageConfig}.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public class TryOnlySampler implements Sampler {

    private static final String HEADER_NAME = "X-Ouroboros-Try";
    private static final String TRY_VALUE = "on";

    /**
     * Determines whether a span should be sampled.
     * <p>
     * First checks the HTTP request header directly (X-Ouroboros-Try: on).
     * If no HTTP context is available (for internal spans), falls back to TryContext.
     * Only samples spans for Try requests to eliminate tracing overhead for normal requests.
     *
     * @param parentContext The parent context (may contain parent span)
     * @param traceId The trace ID
     * @param name The span name
     * @param spanKind The span kind (CLIENT, SERVER, INTERNAL, etc.)
     * @param attributes The span attributes
     * @param parentLinks The parent links
     * @return RECORD_AND_SAMPLE if Try request, DROP otherwise
     */
    @Override
    public SamplingResult shouldSample(Context parentContext,
                                       String traceId,
                                       String name,
                                       SpanKind spanKind,
                                       Attributes attributes,
                                       List<LinkData> parentLinks) {

        log.debug("shouldSample called for span: {}", name);
        boolean isTryRequest = false;

        // Priority 1: Check HTTP header directly (for HTTP spans)
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            log.debug("RequestContextHolder attributes: {}", attrs != null ? "present" : "null");

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String tryHeader = request.getHeader(HEADER_NAME);
                log.debug("X-Ouroboros-Try header value: {}", tryHeader);
                isTryRequest = TRY_VALUE.equalsIgnoreCase(tryHeader);

                if (isTryRequest) {
                    log.info("✅ Try request detected via header - sampling span: {}", name);
                } else {
                    log.debug("❌ Non-Try request (header check) - dropping span: {}", name);
                }

                return isTryRequest ? SamplingResult.recordAndSample() : SamplingResult.drop();
            }
        } catch (Exception e) {
            log.debug("Exception checking HTTP request context: {}", e.getMessage());
        }

        // Priority 2: Fallback to TryContext (for internal/child spans)
        isTryRequest = TryContext.hasTryId();
        log.debug("TryContext.hasTryId(): {}", isTryRequest);

        if (isTryRequest) {
            log.info("✅ Try request detected via TryContext - sampling span: {}", name);
            return SamplingResult.recordAndSample();
        } else {
            log.debug("❌ Non-Try request (TryContext check) - dropping span: {}", name);
            return SamplingResult.drop();
        }
    }

    /**
     * Returns a description of this sampler.
     *
     * @return A human-readable description
     */
    @Override
    public String getDescription() {
        return "TryOnlySampler{samples only Try requests via X-Ouroboros-Try header}";
    }
}