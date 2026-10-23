package jobportal.exception;

/**
 * Base class of all our own CHECKED exceptions.
 * It extends Exception, so the compiler forces us to handle it (try/catch) or declare it (throws).
 *
 * Hierarchy:  Throwable -> Exception -> JobPortalException -> DuplicateRecordException
 */
public class JobPortalException extends Exception {

    public JobPortalException(String message) {
        super(message);   // super = parent class (Exception) stores the message
    }
}
