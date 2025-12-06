/**
 * Service layer for Try feature business logic.
 * <p>
 * This package contains protocol-agnostic services that provide business logic
 * for retrieving and analyzing Try execution results. Services can be used by
 * both REST and WebSocket endpoints.
 * <p>
 * <b>Services:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.service.TrySummaryService} - Retrieves summary metadata</li>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.service.TryMethodListService} - Retrieves paginated method list</li>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.service.TryTraceService} - Retrieves full call trace</li>
 *   <li>{@link kr.co.ouroboros.core.global.tryit.service.TryIssuesService} - Retrieves detected issues</li>
 * </ul>
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.global.tryit.service;

