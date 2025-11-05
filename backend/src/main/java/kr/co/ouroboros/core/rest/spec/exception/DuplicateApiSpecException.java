package kr.co.ouroboros.core.rest.spec.exception;

/**
 * Exception thrown when attempting to create a REST API specification
 * with a path and method combination that already exists.
 */
public class DuplicateApiSpecException extends RuntimeException {

    private final String path;
    private final String method;

    public DuplicateApiSpecException(String path, String method) {
        super(String.format("API specification already exists for %s %s", method.toUpperCase(), path));
        this.path = path;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }
}
