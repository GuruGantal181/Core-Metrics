package coremetrics.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Shared JDBC connection configuration for all DAO classes. */
public final class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/core_metrics?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "181106";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("coremetrics.db.url", "CORE_METRICS_DB_URL", DEFAULT_URL);
        String username = setting("coremetrics.db.username", "CORE_METRICS_DB_USERNAME", DEFAULT_USER);
        String password = setting("coremetrics.db.password", "CORE_METRICS_DB_PASSWORD", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String systemProperty, String environmentVariable, String fallback) {
        String systemValue = System.getProperty(systemProperty);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return fallback;
    }
}
