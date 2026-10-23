package jobportal.exception;

/**
 * Thrown when something already exists, for example:
 * - registering with an email that is already used
 * - applying twice for the same job
 * - saving the same job twice
 */
public class DuplicateRecordException extends JobPortalException {

    public DuplicateRecordException(String message) {
        super(message);
    }
}
