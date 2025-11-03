package kr.co.ouroboros.core.rest.spec.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data wrapper for YAML import validation errors.
 * <p>
 * Used as the {@code data} field in error responses when YAML validation fails.
 * Contains detailed validation errors to help users fix their YAML files.
 *
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportValidationErrorData {
    /**
     * List of validation errors found in the uploaded YAML file.
     * Each error includes location, error code, and descriptive message.
     */
    private List<ValidationError> validationErrors;
}