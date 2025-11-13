/**
 * Request identification components for Try feature in STOMP.
 * <p>
 * This package contains components that identify Try requests in STOMP messages
 * and set tryId in STOMP headers and OpenTelemetry context.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.identification.TryStompChannelInterceptor} - Interceptor for identifying Try requests and setting tryId in context for inbound messages</li>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.identification.TryStompOutboundChannelInterceptor} - Interceptor for adding tryId header to outbound messages</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.websocket.tryit.identification;

