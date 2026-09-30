package lumeva.profileservice.infrastructure.adapter.in.rest.excpetion;


import lombok.extern.slf4j.Slf4j;
import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.MissingHeaderException;
import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ProblemDetail handleProfileNotFound(ProfileNotFoundException ex) {
        log.warn("Profile not found: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Profile Not Found", ex.getMessage(), "profile-not-found", null);
    }

    @ExceptionHandler(ProfileBlockedException.class)
    public ProblemDetail handleProfileBlocked(ProfileBlockedException ex) {
        log.warn("Access attempt to blocked profile: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Profile Blocked", ex.getMessage(), "profile-blocked", null);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        log.warn("Domain validation failure: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, "Business Rule Violation", ex.getMessage(), "business-rule-violation", null);
    }

    @ExceptionHandler(MissingHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingHeaderException ex) {
        log.warn("Bad request header: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Missing or Invalid Header", ex.getMessage(), "invalid-header", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Forbidden", "Access Denied: Insufficient permissions", "access-denied", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        log.warn("DTO validation failed: {}", errors);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Request Content", "Validation failed for request body", "invalid-request", Map.of("errors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "internal-error", null);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, String errorTypeCode, Map<String, Object> extraProperties) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://lumeva.com/errors/" + errorTypeCode));
        problemDetail.setProperty("timestamp", Instant.now());
        if (extraProperties != null) {
            extraProperties.forEach(problemDetail::setProperty);
        }
        return problemDetail;
    }
}