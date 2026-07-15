package com.gft.products.error;

import com.gft.products.similarproducts.application.port.out.ProductNotFoundException;
import com.gft.products.similarproducts.application.port.out.UpstreamUnavailableException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain/infrastructure failures into a homogeneous {@link ErrorResponse}.
 * Internal details (stack traces, upstream messages) are logged but never leaked to the client.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Product not found"));
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    ResponseEntity<ErrorResponse> handleUpstreamUnavailable(UpstreamUnavailableException e) {
        log.error("Upstream dependency failure", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse("Upstream service unavailable"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleValidationFailure(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal server error"));
    }
}
