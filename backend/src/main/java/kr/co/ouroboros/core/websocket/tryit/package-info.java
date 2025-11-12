/**
 * Try feature for STOMP-based real-time communication.
 * <p>
 * This module extends the same identification and tracing strategy used in the HTTP Try module
 * to STOMP messages, enabling developers to experience the same Try functionality in real-time channels.
 * <p>
 * <b>Key Components:</b>
 * <ul>
 *   <li><b>Identification</b> - Inspects STOMP frame headers to detect Try requests and issue identifiers (tryId)</li>
 *   <li><b>Config</b> - Registers channel interceptors with the message broker to apply Try context to inbound/outbound flows</li>
 *   <li><b>Infrastructure</b> - Technical foundation for extending STOMP-specific storage and tracing logic</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.websocket.tryit;


