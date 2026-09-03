package com.edgareldy.springboottutorial.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Unit tests verifying that {@link GlobalExceptionHandler} maps each
 * exception type to the HTTP status and {@link ApiResponse}-wrapped
 * {@link ProblemDetail} shape the README's error-handling contract
 * requires.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundMapsTo404() {
        HttpServletRequest request = mockRequest("/api/categories/99");

        ResponseEntity<ApiResponse<ProblemDetail>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Category not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Category not found");
        assertThat(response.getBody().data().getInstance()).isEqualTo(URI.create("/api/categories/99"));
        assertThat(response.getBody().data().getStatus()).isEqualTo(404);
        assertThat(response.getBody().data().getDetail()).isEqualTo("Category not found");
    }

    @Test
    void businessRuleMapsTo422() {
        HttpServletRequest request = mockRequest("/api/categories/1");

        ResponseEntity<ApiResponse<ProblemDetail>> response = handler.handleBusinessRule(
                new BusinessRuleException("Category still has products"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().data().getStatus()).isEqualTo(422);
    }

    @Test
    void optimisticLockingFailureMapsTo409() {
        HttpServletRequest request = mockRequest("/api/v1/products/7");

        ResponseEntity<ApiResponse<ProblemDetail>> response = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("Row was updated concurrently"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().data().getStatus()).isEqualTo(409);
    }

    @Test
    void validationErrorsMapTo400WithFieldErrors() {
        HttpServletRequest request = mockRequest("/api/products");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "unitPrice", "must be greater than 0"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiResponse<ProblemDetail>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors =
                (Map<String, String>) response.getBody().data().getProperties().get("fieldErrors");
        assertThat(fieldErrors).hasSize(1).containsEntry("unitPrice", "must be greater than 0");
    }

    @Test
    void genericExceptionMapsTo500() {
        HttpServletRequest request = mockRequest("/api/orders");

        ResponseEntity<ApiResponse<ProblemDetail>> response =
                handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().data().getStatus()).isEqualTo(500);
    }

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
