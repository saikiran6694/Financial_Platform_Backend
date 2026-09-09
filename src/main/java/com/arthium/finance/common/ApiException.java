package com.arthium.finance.common;

import org.springframework.http.HttpStatus;

/**
 * Equivalent of FastAPI's HTTPException: carries a status code and a "detail"
 * message, which the global handler renders as {"detail": "..."} so the
 * existing frontend error handling keeps working unchanged.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, detail);
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail);
    }

    public static ApiException internal(String detail) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, detail);
    }
}
