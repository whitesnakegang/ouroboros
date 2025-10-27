package kr.co.ouroboros.core.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper.
 * <p>
 * All API endpoints return this standardized response format containing
 * status code, data, message, and optional error details.
 * <p>
 * Success response example:
 * <pre>
 * {
 *   "status": 200,
 *   "data": {...},
 *   "message": "Success",
 *   "error": null
 * }
 * </pre>
 * <p>
 * Error response example:
 * <pre>
 * {
 *   "status": 500,
 *   "data": null,
 *   "message": "Failed to create API specification",
 *   "error": {
 *     "code": "DUPLICATE_API",
 *     "details": "API specification already exists for GET /api/users"
 *   }
 * }
 * </pre>
 *
 * @param <T> the type of response data
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalApiResponse<T> {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Response data (null on error)
     */
    private T data;

    /**
     * Response message
     */
    private String message;

    /**
     * Error details (null on success)
     */
    private ErrorDetails error;

    /**
     * Creates a successful response.
     *
     * @param data the response data
     * @param message the success message
     * @param <T> the type of response data
     * @return successful API response with status 200
     */
    public static <T> GlobalApiResponse<T> success(T data, String message) {
        return GlobalApiResponse.<T>builder()
                .status(200)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * Creates a successful response with default message.
     *
     * @param data the response data
     * @param <T> the type of response data
     * @return successful API response with status 200
     */
    public static <T> GlobalApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    /**
     * Creates an error response.
     *
     * @param status the HTTP status code
     * @param message the error message
     * @param errorCode the error code
     * @param errorDetails the detailed error description
     * @param <T> the type of response data
     * @return error API response
     */
    public static <T> GlobalApiResponse<T> error(int status, String message, String errorCode, String errorDetails) {
        return GlobalApiResponse.<T>builder()
                .status(status)
                .message(message)
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .details(errorDetails)
                        .build())
                .build();
    }

    /**
     * Creates an error response without error code.
     *
     * @param status the HTTP status code
     * @param message the error message
     * @param <T> the type of response data
     * @return error API response
     */
    public static <T> GlobalApiResponse<T> error(int status, String message) {
        return GlobalApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
}
