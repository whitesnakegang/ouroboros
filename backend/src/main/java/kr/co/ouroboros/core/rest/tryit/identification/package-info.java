/**
 * Request identification components for Try feature.
 * <p>
 * This package contains components that identify Try requests and set tryId
 * in response headers and OpenTelemetry context.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.identification.TryFilter} - Filter for identifying Try requests and setting tryId in context</li>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.identification.TryResponseAdvice} - ResponseBodyAdvice for setting tryId in response headers</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.identification;

