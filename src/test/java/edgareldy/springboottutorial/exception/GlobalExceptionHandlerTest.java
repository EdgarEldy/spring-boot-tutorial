package edgareldy.springboottutorial.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Unit tests verifying that {@link GlobalExceptionHandler} maps each
 * exception type to the HTTP status and {@link ApiResponse} shape the
 * README's error-handling contract requires.
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

        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Category not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Category not found");
        assertThat(response.getBody().data().path()).isEqualTo("/api/categories/99");
        assertThat(response.getBody().data().status()).isEqualTo(404);
    }

    @Test
    void businessRuleMapsTo422() {
        HttpServletRequest request = mockRequest("/api/categories/1");

        ResponseEntity<ApiResponse<ErrorResponse>> response = handler.handleBusinessRule(
                new BusinessRuleException("Category still has products"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().data().status()).isEqualTo(422);
    }

    @Test
    void authenticationExceptionMapsTo401() {
        HttpServletRequest request = mockRequest("/api/v1/auth/login");

        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleAuthentication(new BadCredentialsException("Bad credentials"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().data().status()).isEqualTo(401);
    }

    @Test
    void validationErrorsMapTo400WithFieldErrors() {
        HttpServletRequest request = mockRequest("/api/products");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "unitPrice", "must be greater than 0"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiResponse<ErrorResponse>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().data().fieldErrors()).hasSize(1);
        assertThat(response.getBody().data().fieldErrors().get(0).field()).isEqualTo("unitPrice");
        assertThat(response.getBody().data().fieldErrors().get(0).message()).isEqualTo("must be greater than 0");
    }

    @Test
    void genericExceptionMapsTo500() {
        HttpServletRequest request = mockRequest("/api/orders");

        ResponseEntity<ApiResponse<ErrorResponse>> response =
                handler.handleGeneric(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().data().status()).isEqualTo(500);
    }

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
