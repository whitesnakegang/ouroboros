/**
 * Common trace data models.
 * <p>
 * This package contains common data transfer objects (DTOs) used across
 * different storage backends for representing trace data.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model.TraceDTO} - Common trace data structure</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <p>
 * These models are used by all storage implementations (Tempo, in-memory, etc.)
 * to represent trace data in a unified format. They follow the OpenTelemetry
 * trace data model structure.
 *
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.tryit.infrastructure.storage.model;

