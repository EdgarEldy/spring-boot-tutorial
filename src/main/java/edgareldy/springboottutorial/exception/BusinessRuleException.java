package edgareldy.springboottutorial.exception;

/**
 * Thrown by service implementations when an operation violates a domain
 * rule (e.g. deleting a Category that still has Products attached).
 * Caught by {@link GlobalExceptionHandler} and translated into a 422 response.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
