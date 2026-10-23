package jobportal.exception;

/**
 * Thrown when user input is wrong (empty field, bad email, bad phone ...).
 * It extends RuntimeException, so it is an UNCHECKED exception:
 * the compiler does not force us to catch it.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
