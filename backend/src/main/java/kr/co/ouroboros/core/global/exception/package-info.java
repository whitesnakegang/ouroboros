/**
 * Global exception handling package.
 * <p>
 * This package contains global exception handlers that convert exceptions into
 * standardized {@link kr.co.ouroboros.core.global.response.GlobalApiResponse} format
 * for all Ouroboros SDK endpoints.
 * <p>
 * <b>Exception Handling Architecture:</b>
 * <ol>
 *   <li><b>Package-specific handlers</b> (higher priority) - Handle domain-specific exceptions
 *       <ul>
 *         <li>{@link kr.co.ouroboros.core.rest.spec.exception.RestSpecExceptionHandler} - REST API spec exceptions</li>
 *         <li>{@link kr.co.ouroboros.core.rest.tryit.exception.TryExceptionHandler} - Try endpoint exceptions</li>
 *       </ul>
 *   </li>
 *   <li><b>Global handler</b> (lower priority) - Handles common exceptions for all SDK endpoints
 *       <ul>
 *         <li>{@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler} - Common exceptions (IllegalArgumentException, ClassCastException, NullPointerException, Exception)</li>
 *       </ul>
 *   </li>
 * </ol>
 * <p>
 * <b>Key Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.global.exception.GlobalExceptionHandler} - Global exception handler for all SDK endpoints</li>
 * </ul>
 * <p>
 * <b>Note:</b> The global exception handler applies to all controllers in the application.
 * Package-specific handlers take precedence for their respective packages.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.global.exception;