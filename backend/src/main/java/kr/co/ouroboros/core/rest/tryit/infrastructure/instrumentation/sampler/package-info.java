/**
 * OpenTelemetry sampling strategies for Try requests.
 * <p>
 * This package contains custom Sampler implementations that control which
 * spans are created and exported to tracing backends (Tempo).
 * <p>
 * <b>Key Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.sampler.TryOnlySampler} -
 *       Samples only Try requests, preventing span creation for normal requests</li>
 * </ul>
 * <p>
 * <b>Sampler vs SpanProcessor:</b>
 * <ul>
 *   <li><b>Sampler</b>: Runs BEFORE span creation, can prevent span creation entirely</li>
 *   <li><b>SpanProcessor</b>: Runs AFTER span creation, can only modify spans</li>
 * </ul>
 * <p>
 * <b>How TryOnlySampler Works:</b>
 * <ol>
 *   <li>HTTP request arrives</li>
 *   <li>Sampler.shouldSample() called (before TryFilter)</li>
 *   <li>Checks X-Ouroboros-Try header directly</li>
 *   <li>If "on" → RECORD_AND_SAMPLE (creates span)</li>
 *   <li>If missing → DROP (no span created, no Tempo storage)</li>
 *   <li>Fallback: Checks TryContext for internal spans</li>
 * </ol>
 * <p>
 * <b>Configuration:</b>
 * TryOnlySampler is registered as a @Primary bean in
 * {@link kr.co.ouroboros.core.rest.tryit.config.TraceStorageConfig} with
 * @AutoConfigureBefore to ensure it takes precedence over Spring Boot's default Sampler.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.instrumentation.sampler;