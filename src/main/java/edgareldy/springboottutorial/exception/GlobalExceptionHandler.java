package edgareldy.springboottutorial.exception;

import edgareldy.springboottutorial.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler that translates every exception thrown by a
 * controller (directly or through a service) into a consistent
 * {@code ApiResponse<ProblemDetail>}, so callers never have to parse a
 * different error shape depending on which endpoint failed. The error
 * payload itself is Spring 6's built-in {@link ProblemDetail} (RFC 7807:
 * {@code type}/{@code title}/{@code status}/{@code detail}/{@code instance}),
 * still wrapped in the project's {@link ApiResponse} envelope, not a
 * replacement for it.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ProblemDetail>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<ProblemDetail>> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ProblemDetail>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ProblemDetail>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
    }

    private ResponseEntity<ApiResponse<ProblemDetail>> buildResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        if (fieldErrors != null) {
            problemDetail.setProperty("fieldErrors", fieldErrors);
        }
        ApiResponse<ProblemDetail> body = new ApiResponse<>(false, message, problemDetail, Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}
