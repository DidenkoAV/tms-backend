package com.test.system.controller.advice;

import com.test.system.dto.error.ErrorResponse;
import com.test.system.exceptions.auth.PasswordRateLimitException;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.exceptions.jira.JiraApiException;
import com.test.system.exceptions.jira.JiraNotFoundException;
import com.test.system.exceptions.run.RunClosedException;
import com.test.system.exceptions.testcase.TestCaseExportException;
import com.test.system.exceptions.testcase.TestCaseImportException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global exception handler for REST API.
 * Catches all exceptions from controllers and converts them to standardized JSON responses.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiExceptionHandler {

    private static final String LOG_PREFIX = "[API-EX]";

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Build ResponseEntity with BAD_REQUEST status.
     */
    private ResponseEntity<ErrorResponse> badRequest(String error, String message) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(error, message));
    }

    /**
     * Build ResponseEntity with custom status.
     */
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(error, message));
    }

    /**
     * Build ResponseEntity with validation errors.
     */
    private ResponseEntity<ErrorResponse> validationError(Map<String, String> errors) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation_error", "Validation failed", errors));
    }

    /**
     * Log warning with context.
     */
    private void logWarn(int status, String context, String message) {
        log.warn("{} {} {}: {}", LOG_PREFIX, status, context, message);
    }

    /**
     * Log error with context and exception.
     */
    private void logError(int status, String context, String message, Exception ex) {
        log.error("{} {} {}: {}", LOG_PREFIX, status, context, message, ex);
    }

    // ========================================================================
    // 400 Bad Request family
    // ========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        logWarn(400, "Bad Request", ex.getMessage());
        return badRequest("bad_request", ex.getMessage());
    }

    @ExceptionHandler(RunClosedException.class)
    public ResponseEntity<ErrorResponse> handleRunClosed(RunClosedException ex) {
        logWarn(400, "Run Closed", ex.getMessage());
        return badRequest("run_closed", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        logWarn(400, "Unreadable Request", "Malformed JSON or unreadable request body");
        return badRequest("bad_request", "Malformed JSON or unreadable request body");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

        logWarn(400, "Validation Failed", "Body validation errors: " + errors);
        return validationError(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.put(v.getPropertyPath().toString(), v.getMessage());
        }

        logWarn(400, "Constraint Violations", "Validation errors: " + errors);
        return validationError(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue());
        logWarn(400, "Type Mismatch", msg);
        return badRequest("bad_request", msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "Missing required parameter: " + ex.getParameterName();
        logWarn(400, "Missing Parameter", msg);
        return badRequest("bad_request", msg);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        String msg = "Missing required header: " + ex.getHeaderName();
        logWarn(400, "Missing Header", msg);
        return badRequest("bad_request", msg);
    }

    // ========================================================================
    // 401 / 403 Authentication & Authorization
    // ========================================================================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        logWarn(401, "Unauthorized", ex.getMessage());
        return response(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        logWarn(401, "Unauthorized", "Invalid credentials");
        return response(HttpStatus.UNAUTHORIZED, "unauthorized", "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        logWarn(403, "Forbidden", "Account disabled or email not verified");
        return response(HttpStatus.FORBIDDEN, "forbidden", "EMAIL_NOT_VERIFIED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        logWarn(403, "Forbidden", ex.getMessage());
        return response(HttpStatus.FORBIDDEN, "forbidden", "Access denied");
    }

    // ========================================================================
    // 404 Not Found
    // ========================================================================

    @ExceptionHandler({
            NotFoundException.class,
            ChangeSetPersister.NotFoundException.class,
            EntityNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Not found";
        logWarn(404, "Not Found", msg);
        return response(HttpStatus.NOT_FOUND, "not_found", msg);
    }

    // ========================================================================
    // 405 Method Not Allowed
    // ========================================================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logWarn(405, "Method Not Allowed", "Method: " + ex.getMethod());
        return response(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "Method not allowed");
    }

    // ========================================================================
    // 409 Conflict (DB constraints, unique, FK, etc.)
    // ========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Conflict";
        logWarn(409, "Conflict", msg);
        return response(HttpStatus.CONFLICT, "conflict", msg);
    }

    // ========================================================================
    // 429 Too Many Requests
    // ========================================================================

    @ExceptionHandler(PasswordRateLimitException.class)
    public ResponseEntity<ErrorResponse> handlePasswordRateLimit(PasswordRateLimitException ex) {
        logWarn(429, "Too Many Requests", ex.getMessage());
        return response(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", ex.getMessage());
    }

    // ========================================================================
    // Jira Integration (404 / 500)
    // ========================================================================

    @ExceptionHandler(JiraNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJiraNotFound(JiraNotFoundException ex) {
        logWarn(404, "Jira Not Found", ex.getMessage());
        return response(HttpStatus.NOT_FOUND, "jira_not_found", ex.getMessage());
    }

    @ExceptionHandler(JiraApiException.class)
    public ResponseEntity<ErrorResponse> handleJiraApi(JiraApiException ex) {
        logError(500, "Jira API Error", ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "jira_api_error", ex.getMessage());
    }

    // ========================================================================
    // TestCase Import/Export (500)
    // ========================================================================

    @ExceptionHandler(TestCaseImportException.class)
    public ResponseEntity<ErrorResponse> handleTestCaseImport(TestCaseImportException ex) {
        logError(500, "TestCase Import Error", ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "testcase_import_error", ex.getMessage());
    }

    @ExceptionHandler(TestCaseExportException.class)
    public ResponseEntity<ErrorResponse> handleTestCaseExport(TestCaseExportException ex) {
        logError(500, "TestCase Export Error", ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "testcase_export_error", ex.getMessage());
    }

    // ========================================================================
    // ResponseStatusException Passthrough
    // ========================================================================

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String reason = ex.getReason() != null ? ex.getReason() : "Error";

        String key = switch (status.value()) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 429 -> "rate_limit";
            default -> "error";
        };

        if (status.is5xxServerError()) {
            logError(status.value(), "ResponseStatusException", reason, ex);
        } else if (status.is4xxClientError()) {
            logWarn(status.value(), "ResponseStatusException", reason);
        } else {
            log.info("{} {} ResponseStatusException: {}", LOG_PREFIX, status.value(), reason);
        }

        return response(status, key, reason);
    }

    // ========================================================================
    // 500 Fallback (Catch-All)
    // ========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        logError(500, "Unexpected Error", ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Internal error");
    }
}
