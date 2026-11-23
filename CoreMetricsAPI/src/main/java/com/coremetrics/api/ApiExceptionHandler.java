package com.coremetrics.api;

import coremetrics.dao.DatabaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Map<String, String>> handleDatabaseFailure(DatabaseException exception) {
        LOGGER.error("Database request failed.", exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Throwable cause = exception.getCause();
        if (cause instanceof SQLException sqlException
                && sqlException.getSQLState() != null
                && sqlException.getSQLState().startsWith("23")) {
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(Map.of(
                "error", "The database could not complete the request. Check the backend console and database connection."
        ));
    }
}
