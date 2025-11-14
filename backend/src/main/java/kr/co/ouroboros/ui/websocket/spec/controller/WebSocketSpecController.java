package kr.co.ouroboros.ui.websocket.spec.controller;

import kr.co.ouroboros.core.global.response.GlobalApiResponse;
import kr.co.ouroboros.core.websocket.spec.service.WebSocketOperationService;
import kr.co.ouroboros.core.websocket.spec.validator.ImportWebSocketYamlValidator;
import kr.co.ouroboros.ui.websocket.spec.dto.ImportValidationErrorData;
import kr.co.ouroboros.ui.websocket.spec.dto.ImportYamlResponse;
import kr.co.ouroboros.ui.websocket.spec.dto.ValidationError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST controller for managing WebSocket/STOMP API specifications.
 * <p>
 * Provides import and export operations for AsyncAPI 3.0.0 YAML files.
 * All endpoints are prefixed with {@code /ouro/websocket-specs}.
 * All responses follow the standard {@link GlobalApiResponse} format.
 *
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequestMapping("/ouro/websocket-specs")
@RequiredArgsConstructor
public class WebSocketSpecController {

    private final WebSocketOperationService operationService;
    private final ImportWebSocketYamlValidator importWebSocketYamlValidator;

    /**
     * Imports external AsyncAPI 3.0.0 YAML file and merges into ourowebsocket.yml.
     * <p>
     * Validates the uploaded YAML file, handles duplicate channels, operations, schemas,
     * and messages by auto-renaming, enriches with Ouroboros custom fields, and updates
     * $ref references accordingly.
     * <p>
     * <b>File Requirements:</b>
     * <ul>
     *   <li>Extension: .yml or .yaml</li>
     *   <li>Format: AsyncAPI 3.0.0 specification</li>
     *   <li>Required fields: asyncapi, info (title, version), channels</li>
     *   <li>Valid action types: send, receive</li>
     * </ul>
     * <p>
     * <b>Duplicate Handling:</b>
     * <ul>
     *   <li>Channels: Duplicate channel names are renamed with "-import" suffix</li>
     *   <li>Operations: Duplicate operation names are renamed with "-import" suffix</li>
     *   <li>Schemas: Duplicate schema names are renamed with "-import" suffix</li>
     *   <li>Messages: Duplicate message names are renamed with "-import" suffix</li>
     *   <li>$ref references are automatically updated to point to renamed components</li>
     * </ul>
     * <p>
     * Exceptions are handled by the global exception handler.
     *
     * @param file the AsyncAPI YAML file to import
     * @return import result with counts and renamed items
     * @throws Exception if validation fails or import operation fails
     */
    @PostMapping("/import")
    public ResponseEntity<GlobalApiResponse<?>> importYaml(
            @RequestParam("file") MultipartFile file) throws Exception {

        log.info("Received AsyncAPI import request: filename={}, size={}",
                file.getOriginalFilename(), file.getSize());

        // Step 1: Validate file extension
        String filename = file.getOriginalFilename();
        List<ValidationError> fileErrors = importWebSocketYamlValidator.validateFileExtension(filename);
        if (!fileErrors.isEmpty()) {
            ValidationError error = fileErrors.get(0);
            GlobalApiResponse<ImportYamlResponse> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    error.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Step 2: Read file content
        String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Step 3: Validate YAML content
        List<ValidationError> contentErrors = importWebSocketYamlValidator.validate(yamlContent);
        if (!contentErrors.isEmpty()) {
            // Build error response with validation errors in data field
            ImportValidationErrorData errorData = ImportValidationErrorData.builder()
                    .validationErrors(contentErrors)
                    .build();

            String message = String.format("AsyncAPI validation failed with %d error%s",
                    contentErrors.size(),
                    contentErrors.size() > 1 ? "s" : "");

            GlobalApiResponse<ImportValidationErrorData> response = GlobalApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    message,
                    "VALIDATION_FAILED",
                    contentErrors.size() + " validation error" + (contentErrors.size() > 1 ? "s" : "") + " found in the uploaded YAML file"
            );
            response.setData(errorData);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Step 4: Import YAML
        ImportYamlResponse data = operationService.importYaml(yamlContent);

        GlobalApiResponse<ImportYamlResponse> response = GlobalApiResponse.success(
                data,
                "AsyncAPI import completed successfully"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Exports the AsyncAPI YAML file content.
     * <p>
     * Returns the current saved content from the ourowebsocket.yml file directly.
     * The response is plain text (YAML format) for easy download.
     * <p>
     * Exceptions are handled by the global exception handler.
     *
     * @return YAML file content as plain text
     * @throws Exception if file reading fails or file does not exist
     */
    @GetMapping("/export/yaml")
    public ResponseEntity<String> exportYaml() throws Exception {
        String yamlContent = operationService.exportYaml();
        return ResponseEntity.ok()
                .header("Content-Type", "text/yaml; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"ourowebsocket.yml\"")
                .body(yamlContent);
    }
}