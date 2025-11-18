/**
 * Session management components for Try feature in STOMP.
 * <p>
 * This package contains session management components for tracking
 * and managing Try request mappings between tryId and sessionId.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.infrastructure.session.TrySessionRegistry} - Registry for managing tryId and sessionId mappings</li>
 *   <li>{@link kr.co.ouroboros.core.websocket.tryit.infrastructure.session.TrySessionLifecycleListener} - Event listener for cleaning up Try mappings on session disconnect</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.websocket.tryit.infrastructure.session;

