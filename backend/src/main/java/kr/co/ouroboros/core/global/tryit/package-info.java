/**
 * Global Try feature components shared across protocols.
 * <p>
 * This package contains cross-cutting Try functionality that is used
 * by both REST and WebSocket implementations. It provides protocol-agnostic
 * infrastructure, trace processing, configuration, and service components.
 * <p>
 * <b>Subpackages:</b>
 * <ul>
 *   <li><b>common</b> - Common constants and utilities (e.g., {@link kr.co.ouroboros.core.global.tryit.common.TryHeaders})</li>
 *   <li><b>config</b> - Auto-configuration and properties for Try feature</li>
 *   <li><b>exception</b> - Try-specific exception types</li>
 *   <li><b>infrastructure</b> - Instrumentation, storage, and client implementations</li>
 *   <li><b>trace</b> - Trace processing, analysis, conversion, and tree building components</li>
 *   <li><b>service</b> - Business logic for retrieving and analyzing Try results</li>
 * </ul>
 * <p>
 * <b>Protocol-Specific Packages:</b>
 * <ul>
 *   <li><b>kr.co.ouroboros.core.rest.tryit</b> - REST-specific Try components</li>
 *   <li><b>kr.co.ouroboros.core.websocket.tryit</b> - WebSocket-specific Try components</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.global.tryit;
