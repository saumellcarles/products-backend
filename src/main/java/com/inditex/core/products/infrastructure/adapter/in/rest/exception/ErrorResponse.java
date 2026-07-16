package com.inditex.core.products.infrastructure.adapter.in.rest.exception;

/**
 * Homogeneous error body returned for every non-2xx response.
 */
public record ErrorResponse(String message) {
}
