# Error handling

All errors in the application are handled centrally by `GlobalExceptionHandler`. Every error
response has the same envelope shape as a success response, making client-side handling
uniform.

---

## GlobalExceptionHandler

`GlobalExceptionHandler` is annotated with `@RestControllerAdvice`. Every `@ExceptionHandler`
method returns `ResponseEntity<ApiResponse<ProblemDetail>>`.

Using Spring 6's `ProblemDetail` (RFC 7807) as the `data` field means error responses carry
machine-readable metadata (`type`, `title`, `status`, `detail`, `instance`) in addition to
the human-readable `message` at the envelope level.

---

## Exception types

| Exception | HTTP status | When it is thrown |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found by id (findById, update, delete) |
| `BusinessRuleException` | 422 | Domain constraint violated (delete a category that has products, delete a customer that has orders) |
| `OptimisticLockingFailureException` | 409 | Concurrent update conflict on a versioned entity |
| `AccessDeniedException` | 403 | Authenticated user lacks the required role or permission |
| `AuthenticationException` | 401 | Missing or invalid credentials |
| `MethodArgumentNotValidException` | 400 | Bean Validation constraint violated on a request DTO |
| `Exception` (catch-all) | 500 | Unexpected error |

---

## Response examples

**404 - Resource not found:**

```json
{
  "success": false,
  "message": "Resource not found",
  "data": {
    "type": "about:blank",
    "title": "Not Found",
    "status": 404,
    "detail": "Category with id 99 not found",
    "instance": "/api/v1/categories/99"
  },
  "timestamp": "2025-11-15T10:30:00"
}
```

**400 - Validation error:**

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "Validation failed",
    "instance": "/api/v1/categories",
    "fieldErrors": {
      "name": "Category name must not be blank"
    }
  },
  "timestamp": "2025-11-15T10:30:00"
}
```

The `fieldErrors` map comes from iterating `MethodArgumentNotValidException.getBindingResult()`
and is added as a `ProblemDetail` extension via `problemDetail.setProperty("fieldErrors", ...)`.

---

## Why BusinessRuleException is not 400

A `400 Bad Request` signals that the request itself is malformed - a missing field, a wrong
type, a format violation. The client should fix the request and retry.

A `422 Unprocessable Entity` signals that the request is syntactically correct but cannot be
processed due to a domain constraint. The client must change state in the system before
retrying (for example, delete the products before deleting their category). Using 422
communicates this distinction clearly.

---

## Validation error messages

Bean Validation messages must be written as readable prose, never as the raw field name.

```java
// Correct
@NotBlank(message = "Category name must not be blank")
String name,

// Wrong - exposes implementation detail to clients
@NotBlank(message = "name must not be blank")
String name,
```

The message is returned verbatim in the `fieldErrors` map. It is the only feedback the
client receives about what to fix, so it must be self-explanatory.
