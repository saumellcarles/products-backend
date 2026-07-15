package com.gft.products.error;

/**
 * Homogeneous error body returned for every non-2xx response.
 */
public record ErrorResponse(String message) {
}
