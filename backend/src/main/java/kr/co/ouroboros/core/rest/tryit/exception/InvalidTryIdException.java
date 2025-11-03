package kr.co.ouroboros.core.rest.tryit.exception;

/**
 * Exception thrown when tryId format is invalid (not a valid UUID).
 * <p>
 * This exception is thrown when a tryId parameter does not conform to
 * the UUID format required for Try session identification.
 * <p>
 * Handled by {@link TryExceptionHandler} which converts it to a 400 Bad Request
 * response with error code "INVALID_TRY_ID".
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
public class InvalidTryIdException extends IllegalArgumentException {

    /**
     * The invalid tryId string that caused this exception.
     */
    private final String tryId;

    /**
     * Constructs a new InvalidTryIdException with the specified tryId.
     *
     * @param tryId the invalid tryId string
     */
    public InvalidTryIdException(String tryId) {
        super(String.format("Invalid tryId format: '%s'. tryId must be a valid UUID.", tryId));
        this.tryId = tryId;
    }

    /**
     * Returns the invalid tryId that caused this exception.
     *
     * @return the invalid tryId string
     */
    public String getTryId() {
        return tryId;
    }
}

