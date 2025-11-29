/**
 * Try-specific exception types and handlers.
 * <p>
 * This package contains exception types and handlers for the Try feature.
 * <p>
 * <b>Exceptions:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.exception.InvalidTryIdException} -
 *       Thrown when tryId format is invalid (protocol-agnostic)</li>
 * </ul>
 * <p>
 * <b>Exception Handlers:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.exception.TryExceptionHandler} -
 *       REST controller exception handler (scoped to REST controllers)</li>
 * </ul>
 * <p>
 * <b>Note:</b> TryExceptionHandler is REST-specific due to {@code @RestControllerAdvice},
 * but is kept in global package for cohesion with exception types.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.global.tryit.exception;

