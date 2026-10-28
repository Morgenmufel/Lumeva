package lumeva.profileservice.infrastructure.adapter.in.rest.excpetion;

import lombok.extern.slf4j.Slf4j;
import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.InvalidProfileDataException;
import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Наследуемся от ResponseEntityExceptionHandler: он уже правильно отвечает на ошибки самого Spring MVC
 * (кривой JSON — 400, несуществующий путь — 404, неподдерживаемый метод — 405) в формате ProblemDetail.
 * Наш обработчик Exception.class ловит только то, что осталось, — настоящие 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ProblemDetail handleProfileNotFound(ProfileNotFoundException ex) {
        log.warn("Profile not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Profile Not Found", ex.getMessage(), "profile-not-found");
    }

    @ExceptionHandler(ProfileBlockedException.class)
    public ProblemDetail handleProfileBlocked(ProfileBlockedException ex) {
        log.warn("Access attempt to blocked profile: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Profile Blocked", ex.getMessage(), "profile-blocked");
    }

    @ExceptionHandler(InvalidProfileDataException.class)
    public ProblemDetail handleInvalidProfileData(InvalidProfileDataException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Profile Data", ex.getMessage(), "invalid-profile-data");
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Business Rule Violation", ex.getMessage(), "business-rule-violation");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModification(ObjectOptimisticLockingFailureException ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Concurrent Modification",
                "Profile was changed by another request. Reload it and try again.", "concurrent-modification");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Valid access token required", "unauthorized");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions", "access-denied");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));

        log.warn("Request validation failed: {}", errors);
        ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid Request Content",
                "Validation failed for request body", "invalid-request");
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "internal-error");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://lumeva.com/errors/" + code));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
