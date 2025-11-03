/**
 * Validation and enrichment utilities for ourorest.yml files.
 * <p>
 * This package provides automatic validation of OpenAPI specifications
 * and enrichment with Ouroboros-specific custom fields during application startup.
 * <p>
 * <b>Key Components:</b>
 * <ul>
 *   <li>{@link kr.co.ouroboros.core.rest.spec.validator.OurorestYamlValidator} - Main validator component</li>
 * </ul>
 * <p>
 * <b>Features:</b>
 * <ul>
 *   <li>Automatic file creation with default template</li>
 *   <li>Non-blocking OpenAPI structure validation</li>
 *   <li>Auto-enrichment of operation-level x-ouroboros-* fields</li>
 *   <li>Auto-enrichment of schema-level x-ouroboros-mock and x-ouroboros-orders</li>
 *   <li>Non-destructive updates (preserves existing values)</li>
 *   <li>Fail-safe operation (never crashes application)</li>
 * </ul>
 * <p>
 * <b>Execution Flow:</b>
 * <pre>
 * Application Startup
 *   → ApplicationReadyEvent
 *   → OpenApiDumpOnReady.onReady()
 *   → OurorestYamlValidator.validateAndEnrich()
 *   → Protocol initialization continues
 * </pre>
 *
 * @see kr.co.ouroboros.core.rest.spec.validator.OurorestYamlValidator
 * @see kr.co.ouroboros.core.global.runner.OpenApiDumpOnReady
 * @see kr.co.ouroboros.core.rest.common.yaml.RestApiYamlParser
 * @since 0.0.1
 */
package kr.co.ouroboros.core.rest.spec.validator;