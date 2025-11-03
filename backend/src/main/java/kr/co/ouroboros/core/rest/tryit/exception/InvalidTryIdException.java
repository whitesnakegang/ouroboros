package kr.co.ouroboros.core.rest.tryit.exception;

/**
 * Exception thrown when tryId format is invalid (not a valid UUID).
 */
public class InvalidTryIdException extends IllegalArgumentException {

    private final String tryId;

    public InvalidTryIdException(String tryId) {
        super(String.format("Invalid tryId format: '%s'. tryId must be a valid UUID.", tryId));
        this.tryId = tryId;
    }

    public String getTryId() {
        return tryId;
    }
}

