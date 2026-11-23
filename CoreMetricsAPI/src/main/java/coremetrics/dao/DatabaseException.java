package coremetrics.dao;

/** Unchecked wrapper used to propagate JDBC failures to the REST layer. */
public class DatabaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String operation, Throwable cause) {
        super("Database operation failed: " + operation, cause);
    }
}
